package com.lumera.app.data.player

/** Keep explicit seeks inside a known stream and never retry permanent HTTP failures. */
object PlaybackSeekPolicy {
    fun target(requestedMs: Long, durationMs: Long): Long {
        // Seeking to exactly duration can request a byte range past the file's end.
        val lastPosition = if (durationMs > 0L) (durationMs - 1_000L).coerceAtLeast(0L) else Long.MAX_VALUE
        return requestedMs.coerceIn(0L, lastPosition)
    }

    fun relativeTarget(positionMs: Long, deltaMs: Long, durationMs: Long): Long {
        val position = positionMs.coerceAtLeast(0L)
        val requested = when {
            deltaMs > 0L && position > Long.MAX_VALUE - deltaMs -> Long.MAX_VALUE
            deltaMs < 0L && deltaMs < -position -> 0L
            else -> position + deltaMs
        }
        return target(requested, durationMs)
    }

    fun isRetryableHttpStatus(status: Int): Boolean =
        status == 408 || status == 429 || status in 500..599
}
