package com.example.mainservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent {

    private String eventId;
    private AuthEventType eventType;
    private String clientId;
    private String clientType;
    private String ipAddress;
    private Instant timestamp;
    private boolean success;
    private String failureReason;
    private Map<String, Object> metadata;
}
