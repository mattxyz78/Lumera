package com.lumera.app.data.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lumera.app.data.auth.StremioAuthManager
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.model.WatchHistoryEntity
import com.lumera.app.data.model.WatchlistEntity
import com.lumera.app.data.model.SeriesNextUpEntity
import com.lumera.app.data.repository.AddonRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ProfileConfigurationManagerTest {
    private lateinit var context: Context
    private lateinit var dao: AddonDao
    private lateinit var coordinator: ProfileRuntimeCoordinator
    private lateinit var manager: ProfileConfigurationManager
    private val incomingHistory = mutableListOf<List<WatchHistoryEntity>>()
    private val incomingWatchlists = mutableListOf<List<WatchlistEntity>>()
    private val incomingNextUp = mutableListOf<List<SeriesNextUpEntity>>()

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "profile_snapshots").deleteRecursively()
        context.getSharedPreferences("profile_configuration_prefs", 0).edit().clear().commit()
        incomingHistory.clear(); incomingWatchlists.clear(); incomingNextUp.clear()
        dao = mockk(relaxed = true)
        coordinator = ProfileRuntimeCoordinator()
        every { dao.getAllAddons() } returns flowOf(emptyList())
        every { dao.getAllCatalogConfigs() } returns flowOf(emptyList())
        every { dao.getAllHubRows() } returns flowOf(emptyList())
        every { dao.getAllHubRowItems() } returns flowOf(emptyList())
        every { dao.getWatchHistory() } returns flowOf(emptyList())
        coEvery { dao.getWatchlistOnce() } returns emptyList()
        coEvery { dao.getAllSeriesNextUpOnce() } returns emptyList()
        coEvery { dao.getProfileById(any()) } answers { ProfileEntity(id = firstArg(), name = "Test") }
        coEvery {
            dao.replaceRuntimeState(any(), any(), any(), any(),
                capture(incomingHistory), capture(incomingWatchlists), capture(incomingNextUp))
        } just Runs
        manager = ProfileConfigurationManager(context, dao, mockk<StremioAuthManager>(relaxed = true),
            mockk<AddonRepository>(relaxed = true), coordinator)
    }

    private fun snapshot(profileId: Int, text: String) {
        File(context.filesDir, "profile_snapshots").mkdirs()
        File(context.filesDir, "profile_snapshots/profile_$profileId.json").writeText(text)
    }

    @Test fun missingSnapshotCannotInheritViewingData() = runTest {
        every { dao.getWatchHistory() } returns flowOf(listOf(
            WatchHistoryEntity("tt1", "Person A", null, position = 60_000,
                duration = 120_000, lastWatched = 1, type = "movie")))
        coEvery { dao.getWatchlistOnce() } returns listOf(WatchlistEntity("tt1", "movie", "A", null, 1))
        coEvery { dao.getAllSeriesNextUpOnce() } returns listOf(
            SeriesNextUpEntity("tt2", "A", null, 1, 2, null, updatedAt = 1))
        manager.switchRuntimeState(2)
        assertTrue(incomingHistory.last().isEmpty())
        assertTrue(incomingWatchlists.last().isEmpty())
        assertTrue(incomingNextUp.last().isEmpty())
    }

    @Test fun oldSnapshotsMissingArraysRemainReadable() = runTest {
        snapshot(2, "{}")
        manager.switchRuntimeState(2)
        assertEquals(2, coordinator.session?.profileId)
        assertTrue(incomingWatchlists.last().isEmpty())
    }

    @Test fun corruptSnapshotIsPreservedWithoutReplacingDatabase() = runTest {
        snapshot(2, "{broken")
        try {
            manager.switchRuntimeState(2)
            fail("Corrupt snapshot should be rejected")
        } catch (_: IllegalStateException) { }
        assertEquals("{broken", File(context.filesDir, "profile_snapshots/profile_2.json").readText())
        assertTrue(incomingHistory.isEmpty())
        assertNull(coordinator.session)
    }

    @Test fun interruptedSwitchReplaysSnapshotBeforeCapturingRuntime() = runTest {
        snapshot(2, "{}")
        context.getSharedPreferences("profile_configuration_prefs", 0).edit()
            .putInt("last_active_profile_id", 1).putInt("pending_runtime_switch", 2).commit()
        manager.switchRuntimeState(2)
        assertEquals(2, coordinator.session?.profileId)
        assertEquals(2, manager.getLastActiveProfileId())
        assertFalse(context.getSharedPreferences("profile_configuration_prefs", 0)
            .contains("pending_runtime_switch"))
        verify(exactly = 0) { dao.getWatchHistory() }
    }

    @Test fun activeProfileCannotBeDeleted() = runTest {
        snapshot(1, "{}")
        manager.switchRuntimeState(1)
        try {
            manager.deleteProfileState(1)
            fail("Active profile deletion must be refused")
        } catch (_: IllegalStateException) { }
        coVerify(exactly = 0) { dao.deleteProfile(any()) }
    }

    @Test fun switchingBackRestoresEachPersonsWatchlistHistoryAndNextUp() = runTest {
        var history = emptyList<WatchHistoryEntity>()
        var watchlist = emptyList<WatchlistEntity>()
        var nextUp = emptyList<SeriesNextUpEntity>()
        every { dao.getWatchHistory() } answers { flowOf(history) }
        coEvery { dao.getWatchlistOnce() } answers { watchlist }
        coEvery { dao.getAllSeriesNextUpOnce() } answers { nextUp }
        coEvery { dao.replaceRuntimeState(any(), any(), any(), any(), any(), any(), any()) } answers {
            history = arg(4); watchlist = arg(5); nextUp = arg(6)
        }
        snapshot(1, "{}")
        manager.switchRuntimeState(1)
        val a = WatchHistoryEntity("tt1", "A", null, position = 60_000,
            duration = 120_000, lastWatched = 1, type = "movie")
        history = listOf(a)
        watchlist = listOf(WatchlistEntity("tt1", "movie", "A", null, 1))
        nextUp = listOf(SeriesNextUpEntity("tt2", "A", null, 1, 2, null, updatedAt = 1))
        manager.switchRuntimeState(2)
        assertTrue(history.isEmpty()); assertTrue(watchlist.isEmpty()); assertTrue(nextUp.isEmpty())
        val b = a.copy(title = "B", position = 90_000)
        history = listOf(b)
        watchlist = listOf(WatchlistEntity("tt3", "movie", "B", null, 2))
        manager.switchRuntimeState(1)
        assertEquals(listOf(a), history)
        assertEquals("tt1", watchlist.single().id)
        assertEquals(2, nextUp.single().nextEpisode)
        manager.switchRuntimeState(2)
        assertEquals(listOf(b), history)
        assertEquals("tt3", watchlist.single().id)
        assertTrue(nextUp.isEmpty())
    }

    @Test fun atomicBackupRecoversAnInterruptedFileWrite() = runTest {
        snapshot(2, "{partial")
        File(context.filesDir, "profile_snapshots/profile_2.json.bak").writeText("{}")
        manager.switchRuntimeState(2)
        assertEquals(2, coordinator.session?.profileId)
        assertTrue(incomingHistory.last().isEmpty())
    }
}
