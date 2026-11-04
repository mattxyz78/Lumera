package com.lumera.app.data.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackSeekPolicyTest {
    @Test fun endOfVideoSeeksStayInsideTheFile() {
        assertEquals(599_000L, PlaybackSeekPolicy.target(600_000L, 600_000L))
        assertEquals(599_000L, PlaybackSeekPolicy.relativeTarget(595_000L, 10_000L, 600_000L))
    }
    @Test fun ordinaryForwardAndBackwardSeeksKeepTheRequestedTime() {
        assertEquals(180_000L, PlaybackSeekPolicy.target(180_000L, 600_000L))
        assertEquals(110_000L, PlaybackSeekPolicy.relativeTarget(120_000L, -10_000L, 600_000L))
    }
    @Test fun negativeSeeksStopAtTheBeginning() {
        assertEquals(0L, PlaybackSeekPolicy.target(-1L, 600_000L))
        assertEquals(0L, PlaybackSeekPolicy.relativeTarget(5_000L, -10_000L, 600_000L))
    }
    @Test fun unknownDurationsDoNotResetRequestedPositions() {
        for (duration in listOf(0L, -1L, Long.MIN_VALUE)) {
            assertEquals(300_000L, PlaybackSeekPolicy.target(300_000L, duration))
        }
    }
    @Test fun veryShortClipsNeverProduceNegativeTimestamps() {
        assertEquals(0L, PlaybackSeekPolicy.target(500L, 500L))
    }
    @Test fun extremeRelativeInputsDoNotWrapAround() {
        assertEquals(Long.MAX_VALUE, PlaybackSeekPolicy.relativeTarget(100L, Long.MAX_VALUE, 0L))
        assertEquals(0L, PlaybackSeekPolicy.relativeTarget(100L, Long.MIN_VALUE, 0L))
    }
    @Test fun rejectedByteRangesAndAccessFailuresAreNotAutomaticallyRetried() {
        for (status in listOf(400, 401, 403, 404, 410, 416)) {
            assertFalse("HTTP $status", PlaybackSeekPolicy.isRetryableHttpStatus(status))
        }
    }
    @Test fun temporaryServerFailuresRetainBoundedRetrySupport() {
        for (status in listOf(408, 429, 500, 502, 503, 504)) {
            assertTrue("HTTP $status", PlaybackSeekPolicy.isRetryableHttpStatus(status))
        }
    }
}
