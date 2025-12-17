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

    /**
     * Checks if an event has already been processed.
     *
     * @param eventId the event identifier
     * @return true if the event was already processed, false otherwise
     */
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

    /**
     * Marks an event as processed.
     *
     * @param eventId the event identifier
     */
    public void markEventAsProcessed(String eventId) {
        Cache cache = cacheManager.getCache("processedEvents");
        if (cache != null) {
            ProcessedEvent processed = new ProcessedEvent(eventId, Instant.now());
            cache.put(eventId, processed);
            log.debug("Marked event {} as processed", eventId);
        }
    }

    /**
     * Atomically checks if an event has been processed and marks it as processed if not.
     * This method prevents race conditions where two threads could both pass the idempotency
     * check and process the same event twice.
     *
     * @param eventId the event identifier
     * @return true if the event was already processed (caller should skip processing),
     *         false if the event is new and has been marked as processed (caller should process)
     */
    public boolean checkAndMarkProcessed(String eventId) {
        Cache cache = cacheManager.getCache("processedEvents");
        if (cache != null) {
            ProcessedEvent newEntry = new ProcessedEvent(eventId, Instant.now());
            Cache.ValueWrapper existing = cache.putIfAbsent(eventId, newEntry);
            if (existing != null) {
                log.debug("Event {} already processed, skipping", eventId);
                return true;
            }
            log.debug("Event {} marked as processed", eventId);
            return false;
        }
        return false;
    }

    /**
     * Represents a processed event entry for idempotency tracking.
     * Implements Serializable to support distributed caching scenarios.
     */
    private static class ProcessedEvent implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

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
