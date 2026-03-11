package com.currencyconverter.mainservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenEvent {

    private String eventId;
    private TokenEventType eventType;
    private String tokenId;
    private String clientId;
    private String clientType;
    private Instant timestamp;
    private Instant tokenExpiry;
    private String ipAddress;

    public enum TokenEventType {
        GENERATED,
        REFRESHED,
        REVOKED,
        VALIDATED,
        EXPIRED
    }
}
