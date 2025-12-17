package com.example.mainservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class IdempotencyService {

    private final CacheManager cacheManager;

    public IdempotencyService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public boolean isEventProcessed(String eventId) {
        Cache cache = cacheManager.getCache("processedEvents");
        if (cache != null) {
            ProcessedEvent processed = cache.get(eventId, ProcessedEvent.class);
            if (processed != null) {
                log.debug("Event {} already processed at {}", eventId, processed.getTimestamp());
                return true;
            }
        }
        return false;
    }

    public void markEventAsProcessed(String eventId) {
        Cache cache = cacheManager.getCache("processedEvents");
        if (cache != null) {
            ProcessedEvent processed = new ProcessedEvent(eventId, Instant.now());
            cache.put(eventId, processed);
            log.debug("Marked event {} as processed", eventId);
        }
    }

    private static class ProcessedEvent {
        private final String eventId;
        private final Instant timestamp;

        public ProcessedEvent(String eventId, Instant timestamp) {
            this.eventId = eventId;
            this.timestamp = timestamp;
        }

        public String getEventId() {
            return eventId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
