package com.example.mainservice.service;

import com.example.mainservice.event.AuthEvent;
import com.example.mainservice.event.TokenEvent;
import com.example.mainservice.model.UserSession;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for managing user sessions and enforcing security policies.
 *
 * <p>Handles authentication events from Kafka, tracks login attempts, and implements
 * account locking after repeated failed authentications. Sessions are stored in cache
 * with per-client locking for thread-safe operations.</p>
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code session.max-failed-attempts} - Failed attempts before account lock (default: 5)</li>
 *   <li>{@code session.lock-duration-seconds} - Lock duration in seconds (default: 300)</li>
 *   <li>{@code session.alert-threshold} - Failed attempts before security alert (default: 3)</li>
 * </ul>
 */
@Slf4j
@Service
public class SessionManagementService {

    private final CacheManager cacheManager;
    private final SecurityAuditService securityAuditService;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter tokenGeneratedCounter;
    private final Counter tokenRevokedCounter;
    private final Counter sessionLockedCounter;

    // Per-client locks to ensure thread-safe cache operations
    private final ConcurrentHashMap<String, Object> clientLocks = new ConcurrentHashMap<>();

    @Value("${session.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${session.lock-duration-seconds:300}")
    private long lockDurationSeconds;

    @Value("${session.alert-threshold:3}")
    private int alertThreshold;

    public SessionManagementService(CacheManager cacheManager,
                                   SecurityAuditService securityAuditService,
                                   MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        this.securityAuditService = securityAuditService;

        // Initialize metrics
        this.loginSuccessCounter = Counter.builder("auth.login.success")
                .description("Number of successful login attempts")
                .register(meterRegistry);
        this.loginFailureCounter = Counter.builder("auth.login.failed")
                .description("Number of failed login attempts")
                .register(meterRegistry);
        this.tokenGeneratedCounter = Counter.builder("auth.token.generated")
                .description("Number of tokens generated")
                .register(meterRegistry);
        this.tokenRevokedCounter = Counter.builder("auth.token.revoked")
                .description("Number of tokens revoked")
                .register(meterRegistry);
        this.sessionLockedCounter = Counter.builder("auth.session.locked")
                .description("Number of sessions locked")
                .register(meterRegistry);
    }

    /**
     * Processes a successful login event. Resets failed attempt counter and unlocks
     * the account if the lock has expired.
     *
     * @param event the authentication event containing client and login details
     */
    public void handleLoginSuccess(AuthEvent event) {
        log.info("Processing successful login for client: {}", event.getClientId());

        // Synchronize on client-specific lock to ensure atomic get-modify-put
        synchronized (getClientLock(event.getClientId())) {
            UserSession session = getOrCreateSession(event.getClientId());
            session.setLastLoginTime(event.getTimestamp());
            session.setLastLoginIp(event.getIpAddress());
            session.setClientType(event.getClientType());
            session.resetFailedAttempts();

            // Unlock if previously locked and lock expired
            if (session.isLocked() && session.isLockExpired()) {
                session.unlockAccount();
                log.info("Account unlocked for client: {}", event.getClientId());
            }

            updateSession(event.getClientId(), session);
        }

        loginSuccessCounter.increment();

        // Audit log
        securityAuditService.logAuthentication(event);
    }

    /**
     * Processes a failed login event. Increments the failed attempt counter and locks
     * the account if {@code maxFailedAttempts} is reached. Triggers security alerts
     * when attempts exceed {@code alertThreshold}.
     *
     * @param event the authentication event containing client and failure details
     */
    public void handleLoginFailure(AuthEvent event) {
        log.warn("Processing failed login for client: {}", event.getClientId());

        // Synchronize on client-specific lock to ensure atomic get-modify-put
        synchronized (getClientLock(event.getClientId())) {
            UserSession session = getOrCreateSession(event.getClientId());
            session.incrementFailedAttempts();
            session.setClientType(event.getClientType());

            loginFailureCounter.increment();

            // Lock account if threshold exceeded
            if (session.getFailedLoginAttempts() >= maxFailedAttempts && !session.isLocked()) {
                session.lockAccount(lockDurationSeconds);
                sessionLockedCounter.increment();
                log.error("Account locked for client: {} after {} failed attempts",
                        event.getClientId(), session.getFailedLoginAttempts());

                // Send alert
                securityAuditService.sendSecurityAlert(event, session);
            } else if (session.getFailedLoginAttempts() >= alertThreshold) {
                // Send warning alert
                log.warn("Warning: Client {} has {} failed login attempts",
                        event.getClientId(), session.getFailedLoginAttempts());
                securityAuditService.sendSecurityAlert(event, session);
            }

            updateSession(event.getClientId(), session);
        }

        // Audit log
        securityAuditService.logAuthentication(event);
    }

    /**
     * Processes a token generation event. Adds the token to the client's active token set.
     *
     * @param event the token event containing client and token details
     */
    public void handleTokenGenerated(TokenEvent event) {
        log.info("Processing token generation for client: {}", event.getClientId());

        // Synchronize on client-specific lock to ensure atomic get-modify-put
        synchronized (getClientLock(event.getClientId())) {
            UserSession session = getOrCreateSession(event.getClientId());
            session.addToken(event.getTokenId());
            session.setClientType(event.getClientType());

            updateSession(event.getClientId(), session);
        }

        tokenGeneratedCounter.increment();
    }

    /**
     * Processes a token revocation event. Removes the token from the client's active token set.
     *
     * @param event the token event containing client and token details
     */
    public void handleTokenRevoked(TokenEvent event) {
        log.info("Processing token revocation for client: {}", event.getClientId());

        // Synchronize on client-specific lock to ensure atomic get-modify-put
        synchronized (getClientLock(event.getClientId())) {
            UserSession session = getOrCreateSession(event.getClientId());
            session.removeToken(event.getTokenId());

            updateSession(event.getClientId(), session);
        }

        tokenRevokedCounter.increment();
    }

    /**
     * Processes a token expiration event. Removes the expired token from the client's active token set.
     *
     * @param event the token event containing client and token details
     */
    public void handleTokenExpired(TokenEvent event) {
        log.info("Processing token expiration for client: {}", event.getClientId());

        // Synchronize on client-specific lock to ensure atomic get-modify-put
        synchronized (getClientLock(event.getClientId())) {
            UserSession session = getOrCreateSession(event.getClientId());
            session.removeToken(event.getTokenId());

            updateSession(event.getClientId(), session);
        }
    }

    /**
     * Retrieves the session for a given client.
     *
     * @param clientId the client identifier
     * @return an Optional containing the session if found, empty otherwise
     */
    public Optional<UserSession> getSession(String clientId) {
        Cache cache = cacheManager.getCache("userSessions");
        if (cache != null) {
            UserSession session = cache.get(clientId, UserSession.class);
            return Optional.ofNullable(session);
        }
        return Optional.empty();
    }

    /**
     * Checks if a client's session is currently locked. Automatically unlocks
     * the session if the lock has expired.
     *
     * @param clientId the client identifier
     * @return true if the session is locked and not expired, false otherwise
     */
    public boolean isSessionLocked(String clientId) {
        // Synchronize on client-specific lock to ensure atomic get-modify-put
        synchronized (getClientLock(clientId)) {
            Optional<UserSession> session = getSession(clientId);
            if (session.isPresent()) {
                UserSession userSession = session.get();
                if (userSession.isLocked()) {
                    // Check if lock expired
                    if (userSession.isLockExpired()) {
                        userSession.unlockAccount();
                        updateSession(clientId, userSession);
                        return false;
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private UserSession getOrCreateSession(String clientId) {
        Cache cache = cacheManager.getCache("userSessions");
        if (cache != null) {
            UserSession session = cache.get(clientId, UserSession.class);
            if (session == null) {
                session = UserSession.builder()
                        .clientId(clientId)
                        .failedLoginAttempts(0)
                        .locked(false)
                        .build();
            }
            return session;
        }
        return UserSession.builder()
                .clientId(clientId)
                .failedLoginAttempts(0)
                .locked(false)
                .build();
    }

    private void updateSession(String clientId, UserSession session) {
        Cache cache = cacheManager.getCache("userSessions");
        if (cache != null) {
            cache.put(clientId, session);
        }
    }

    /**
     * Get or create a client-specific lock object for synchronization.
     * This ensures fine-grained locking per client rather than locking the entire service.
     *
     * @param clientId the client ID to get the lock for
     * @return a lock object unique to this clientId
     */
    private Object getClientLock(String clientId) {
        return clientLocks.computeIfAbsent(clientId, key -> new Object());
    }
}
