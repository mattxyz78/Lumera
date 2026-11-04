package com.lumera.app.ui.player.base

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ExoPlayerSeekErrorTest {
    @Test fun rangeFailureDoesNotRestartVideoOrEraseItsPosition() = checkRangeFailure(false)
    @Test fun wrappedRangeFailureDoesNotRestartVideoOrEraseItsPosition() = checkRangeFailure(true)

    private fun checkRangeFailure(wrapped: Boolean) {
        val backend = ExoPlayerBackend(ApplicationProvider.getApplicationContext())
        val player = mockk<ExoPlayer>(relaxed = true)
        var position = 120_000L
        every { player.currentPosition } answers { position }
        every { player.duration } returns 600_000L
        every { player.seekTo(any<Long>()) } answers { position = firstArg() }
        ExoPlayerBackend::class.java.getDeclaredField("exoPlayer").apply { isAccessible = true }.set(backend, player)
        ExoPlayerBackend::class.java.getDeclaredField("currentSourceId").apply { isAccessible = true }.set(backend, "default")
        val sourceFlow = ExoPlayerBackend::class.java.getDeclaredField("_sourceOptions").apply { isAccessible = true }.get(backend)
        @Suppress("UNCHECKED_CAST")
        (sourceFlow as kotlinx.coroutines.flow.MutableStateFlow<List<PlayerSourceOption>>).value =
            listOf(PlayerSourceOption(id = "default", url = "https://example.invalid/video", label = "Test", title = "Test"))
        val listener = ExoPlayerBackend::class.java.getDeclaredField("playerListener").apply { isAccessible = true }.get(backend) as Player.Listener
        backend.seekTo(300_000L)
        val http = HttpDataSource.InvalidResponseCodeException(416, "Range not satisfiable", null,
            emptyMap(), DataSpec(Uri.parse("https://example.invalid/video")), byteArrayOf())
        // Include a loader wrapper: the response code is not necessarily the direct cause.
        listener.onPlayerError(PlaybackException("Request failed", if (wrapped) java.io.IOException(http) else http, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))
        assertEquals(300_000L, backend.uiState.value.positionMs)
        assertTrue(backend.uiState.value.errorMessage.orEmpty().contains("HTTP 416"))
        verify(exactly = 0) { player.seekTo(0L) }
        verify(exactly = 0) { player.stop() }
        verify(exactly = 0) { player.clearMediaItems() }
        verify(exactly = 0) { player.prepare() }
        backend.release()
    }
}
