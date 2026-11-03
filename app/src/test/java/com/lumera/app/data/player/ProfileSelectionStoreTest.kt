package com.lumera.app.data.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lumera.app.data.profile.ProfileRuntimeCoordinator
import com.lumera.app.data.model.stremio.Stream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ProfileSelectionStoreTest {
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        for (name in listOf("playback_track_selection_prefs", "source_selection_prefs")) {
            context.getSharedPreferences(name, 0).edit().clear().commit()
        }
    }

    @Test fun sameTitleHasIndependentTracksAndSources() = runTest {
        val coordinator = ProfileRuntimeCoordinator()
        val tracks = PlaybackTrackSelectionStore(context, coordinator)
        val sources = SourceSelectionStore(context, coordinator)
        val sourceA = Stream(url = "https://example.com/a.mp4")
        val sourceB = Stream(url = "https://example.com/b.mp4")
        coordinator.exclusive { coordinator.activate(1) }
        tracks.updateSelection("tt1", "english", "off", 100, true, true, true)
        sources.rememberSelection("tt1", sourceA)
        coordinator.exclusive { coordinator.activate(2) }
        assertNull(tracks.getSelection("tt1"))
        assertNull(sources.findPreferredStream("tt1", listOf(sourceA, sourceB)))
        tracks.updateSelection("tt1", "french", "french", 200, true, true, true)
        sources.rememberSelection("tt1", sourceB)
        // Recreate stores to verify persistence, not just in-memory isolation.
        val reopenedTracks = PlaybackTrackSelectionStore(context, coordinator)
        val reopenedSources = SourceSelectionStore(context, coordinator)
        coordinator.exclusive { coordinator.activate(1) }
        assertEquals("english", reopenedTracks.getSelection("tt1")?.audioTrackId)
        assertEquals(sourceA, reopenedSources.findPreferredStream("tt1", listOf(sourceA, sourceB)))
        tracks.clearSelectionsForPrefix("tt1")
        sources.clearSelectionsForPrefix("tt1")
        coordinator.exclusive { coordinator.activate(2) }
        assertEquals("french", reopenedTracks.getSelection("tt1")?.audioTrackId)
        assertEquals(sourceB, reopenedSources.findPreferredStream("tt1", listOf(sourceA, sourceB)))
        tracks.clearProfile(1); sources.clearProfile(1)
        assertNotNull(tracks.getSelection("tt1"))
    }

    @Test fun legacyUnownedChoicesAreNotAssignedToEveryone() = runTest {
        context.getSharedPreferences("playback_track_selection_prefs", 0)
            .edit().putString("audio_tt1", "legacy").commit()
        val coordinator = ProfileRuntimeCoordinator()
        val store = PlaybackTrackSelectionStore(context, coordinator)
        assertNull(store.getSelection("tt1"))
        coordinator.exclusive { coordinator.activate(1) }
        assertNull(store.getSelection("tt1"))
        // A late callback can explicitly update its owner without touching profile 2.
        coordinator.exclusive { coordinator.activate(2) }
        store.updateSelection("tt1", "one", null, updateAudio = true, updateSubtitle = false, profileId = 1)
        assertNull(store.getSelection("tt1"))
        assertEquals("one", store.getSelection("tt1", 1)?.audioTrackId)
    }
}
