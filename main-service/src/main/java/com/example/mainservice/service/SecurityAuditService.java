package com.example.mainservice.service;

import com.example.mainservice.event.AuthEvent;
import com.example.mainservice.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SecurityAuditService {

    public void logAuthentication(AuthEvent event) {
        log.info("Auth Event: type={}, clientId={}, success={}, ip={}, timestamp={}",
                event.getEventType(),
                event.getClientId(),
                event.isSuccess(),
                event.getIpAddress(),
                event.getTimestamp());

        if (!event.isSuccess()) {
            log.warn("Failed authentication attempt: clientId={}, reason={}, ip={}",
                    event.getClientId(),
                    event.getFailureReason(),
                    event.getIpAddress());
        }
    }

    public void sendSecurityAlert(AuthEvent event, UserSession session) {
        // In production, this would send alerts via email, SMS, webhook, etc.
        log.error("SECURITY ALERT: clientId={}, failedAttempts={}, locked={}, ip={}, reason={}",
                event.getClientId(),
                session.getFailedLoginAttempts(),
                session.isLocked(),
                event.getIpAddress(),
                event.getFailureReason());

        // Could integrate with:
        // - Email service
        // - SMS gateway
        // - Slack/Teams webhook
        // - PagerDuty/Opsgenie
        // - SIEM system
    }

    public void logTokenEvent(String eventType, String clientId, String tokenId) {
        log.info("Token Event: type={}, clientId={}, tokenId={}",
                eventType,
                clientId,
                tokenId);
    }
}
