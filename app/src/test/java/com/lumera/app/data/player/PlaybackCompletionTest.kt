package com.lumera.app.data.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackCompletionTest {
    @Test fun unknownDurationNeverCompletes() {
        for (duration in listOf(null, 0L, -1L)) {
            assertFalse(PlaybackCompletion.isCompleted(600_000, duration, 85))
        }
    }
    @Test fun honoursProfileThreshold() {
        assertFalse(PlaybackCompletion.isCompleted(849_999, 1_000_000, 85))
        assertTrue(PlaybackCompletion.isCompleted(850_000, 1_000_000, 85))
        assertFalse(PlaybackCompletion.isCompleted(850_000, 1_000_000, 95))
    }
    @Test fun nearEndAndInvalidPosition() {
        assertTrue(PlaybackCompletion.isCompleted(971_000, 1_000_000, 99))
        assertFalse(PlaybackCompletion.isCompleted(-1, 10_000, 85))
        assertFalse(PlaybackCompletion.isCompleted(0, 10_000, 85))
    }
}
