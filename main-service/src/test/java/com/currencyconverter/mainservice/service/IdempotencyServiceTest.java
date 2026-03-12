package com.currencyconverter.mainservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IdempotencyService Tests")
class IdempotencyServiceTest {

    private CacheManager cacheManager;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager("processedEvents");
        idempotencyService = new IdempotencyService(cacheManager);
    }

    @Test
    @DisplayName("Should return false for unprocessed event")
    void testIsEventProcessed_ReturnsFalseForUnprocessedEvent() {
        // Arrange
        String eventId = "event-123";

        // Act
        boolean result = idempotencyService.isEventProcessed(eventId);

        // Assert
        assertFalse(result, "Should return false for event that hasn't been processed");
    }

    @Test
    @DisplayName("Should return true for processed event")
    void testIsEventProcessed_ReturnsTrueForProcessedEvent() {
        // Arrange
        String eventId = "event-456";
        idempotencyService.markEventAsProcessed(eventId);

        // Act
        boolean result = idempotencyService.isEventProcessed(eventId);

        // Assert
        assertTrue(result, "Should return true for event that has been processed");
    }

    @Test
    @DisplayName("Should mark event as processed")
    void testMarkEventAsProcessed_StoresEventInCache() {
        // Arrange
        String eventId = "event-789";

        // Act
        idempotencyService.markEventAsProcessed(eventId);

        // Assert
        assertTrue(idempotencyService.isEventProcessed(eventId),
                "Event should be marked as processed and retrievable");
    }

    @Test
    @DisplayName("Should handle multiple events independently")
    void testMultipleEvents_AreHandledIndependently() {
        // Arrange
        String eventId1 = "event-001";
        String eventId2 = "event-002";
        String eventId3 = "event-003";

        // Act
        idempotencyService.markEventAsProcessed(eventId1);
        idempotencyService.markEventAsProcessed(eventId3);

        // Assert
        assertTrue(idempotencyService.isEventProcessed(eventId1));
        assertFalse(idempotencyService.isEventProcessed(eventId2));
        assertTrue(idempotencyService.isEventProcessed(eventId3));
    }

    @Test
    @DisplayName("Should handle duplicate marking gracefully")
    void testMarkEventAsProcessed_HandlesDuplicateMarking() {
        // Arrange
        String eventId = "event-duplicate";

        // Act - Mark the same event twice
        idempotencyService.markEventAsProcessed(eventId);
        idempotencyService.markEventAsProcessed(eventId); // Second time

        // Assert - Should still be marked as processed
        assertTrue(idempotencyService.isEventProcessed(eventId));
    }

    @Test
    @DisplayName("Should return false when cache is null")
    void testIsEventProcessed_ReturnsFalseWhenCacheIsNull() {
        // Arrange - Create service with cache manager that has no "processedEvents" cache
        CacheManager emptyCacheManager = new ConcurrentMapCacheManager("otherCache");
        IdempotencyService serviceWithNoCache = new IdempotencyService(emptyCacheManager);

        // Act
        boolean result = serviceWithNoCache.isEventProcessed("event-nocache");

        // Assert
        assertFalse(result, "Should return false when cache is not available");
    }

    @Test
    @DisplayName("Should handle null cache gracefully when marking event")
    void testMarkEventAsProcessed_HandlesNullCacheGracefully() {
        // Arrange - Create service with cache manager that has no "processedEvents" cache
        CacheManager emptyCacheManager = new ConcurrentMapCacheManager("otherCache");
        IdempotencyService serviceWithNoCache = new IdempotencyService(emptyCacheManager);

        // Act - Should not throw exception
        assertDoesNotThrow(() -> serviceWithNoCache.markEventAsProcessed("event-nocache"));
    }

    @Test
    @DisplayName("Should handle empty event ID")
    void testMarkEventAsProcessed_HandlesEmptyEventId() {
        // Arrange
        String emptyEventId = "";

        // Act
        idempotencyService.markEventAsProcessed(emptyEventId);

        // Assert
        assertTrue(idempotencyService.isEventProcessed(emptyEventId));
    }

    @Test
    @DisplayName("Should handle event IDs with special characters")
    void testMarkEventAsProcessed_HandlesSpecialCharacters() {
        // Arrange
        String specialEventId = "event-@#$%^&*()-uuid-123-abc";

        // Act
        idempotencyService.markEventAsProcessed(specialEventId);

        // Assert
        assertTrue(idempotencyService.isEventProcessed(specialEventId));
    }

    @Test
    @DisplayName("Should maintain idempotency across multiple checks")
    void testIdempotency_MaintainedAcrossMultipleChecks() {
        // Arrange
        String eventId = "event-persistent";
        idempotencyService.markEventAsProcessed(eventId);

        // Act & Assert - Check multiple times
        assertTrue(idempotencyService.isEventProcessed(eventId));
        assertTrue(idempotencyService.isEventProcessed(eventId));
        assertTrue(idempotencyService.isEventProcessed(eventId));
    }

    @Test
    @DisplayName("Should return false and mark as processed for new event using atomic check")
    void testCheckAndMarkProcessed_ReturnsFalseForNewEvent() {
        // Arrange
        String eventId = "event-atomic-new";

        // Act
        boolean result = idempotencyService.checkAndMarkProcessed(eventId);

        // Assert
        assertFalse(result, "Should return false for new event (not already processed)");
        assertTrue(idempotencyService.isEventProcessed(eventId), "Event should now be marked as processed");
    }

    @Test
    @DisplayName("Should return true for already processed event using atomic check")
    void testCheckAndMarkProcessed_ReturnsTrueForProcessedEvent() {
        // Arrange
        String eventId = "event-atomic-existing";
        idempotencyService.markEventAsProcessed(eventId);

        // Act
        boolean result = idempotencyService.checkAndMarkProcessed(eventId);

        // Assert
        assertTrue(result, "Should return true for event that has already been processed");
    }

    @Test
    @DisplayName("Should return true on second atomic check for same event")
    void testCheckAndMarkProcessed_ReturnsTrueOnSecondCall() {
        // Arrange
        String eventId = "event-atomic-double";

        // Act
        boolean firstResult = idempotencyService.checkAndMarkProcessed(eventId);
        boolean secondResult = idempotencyService.checkAndMarkProcessed(eventId);

        // Assert
        assertFalse(firstResult, "First call should return false (new event)");
        assertTrue(secondResult, "Second call should return true (already processed)");
    }

    @Test
    @DisplayName("Should handle null cache gracefully in atomic check")
    void testCheckAndMarkProcessed_HandlesNullCacheGracefully() {
        // Arrange - Create service with cache manager that has no "processedEvents" cache
        CacheManager emptyCacheManager = new ConcurrentMapCacheManager("otherCache");
        IdempotencyService serviceWithNoCache = new IdempotencyService(emptyCacheManager);

        // Act
        boolean result = serviceWithNoCache.checkAndMarkProcessed("event-nocache");

        // Assert
        assertFalse(result, "Should return false when cache is not available");
    }
}
