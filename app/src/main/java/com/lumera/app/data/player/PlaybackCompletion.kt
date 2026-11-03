package com.lumera.app.data.player

object PlaybackCompletion {
    fun isCompleted(positionMs: Long, durationMs: Long?, thresholdPercent: Int): Boolean {
        if (durationMs == null || durationMs <= 0L || positionMs < 5_000L) return false
        val position = positionMs.coerceAtLeast(0L)
        return position.toDouble() / durationMs >= thresholdPercent.coerceIn(50, 99) / 100.0 ||
            durationMs - position <= 30_000L
    }
}
