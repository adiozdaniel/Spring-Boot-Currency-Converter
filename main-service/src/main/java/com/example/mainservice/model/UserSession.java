package com.example.mainservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    private String clientId;
    private String clientType;
    private Set<String> activeTokens;
    private Instant lastLoginTime;
    private String lastLoginIp;
    private int failedLoginAttempts;
    private Instant lastFailedLoginTime;
    private boolean locked;
    private Instant lockExpiry;

    public void addToken(String tokenId) {
        if (activeTokens == null) {
            activeTokens = new HashSet<>();
        }
        activeTokens.add(tokenId);
    }

    public void removeToken(String tokenId) {
        if (activeTokens != null) {
            activeTokens.remove(tokenId);
        }
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLoginTime = Instant.now();
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginTime = null;
    }

    public void lockAccount(long durationSeconds) {
        this.locked = true;
        this.lockExpiry = Instant.now().plusSeconds(durationSeconds);
    }

    public void unlockAccount() {
        this.locked = false;
        this.lockExpiry = null;
    }

    public boolean isLockExpired() {
        return locked && lockExpiry != null && Instant.now().isAfter(lockExpiry);
    }
}
