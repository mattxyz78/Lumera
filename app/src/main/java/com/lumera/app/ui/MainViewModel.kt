package com.lumera.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.profile.ProfileConfigurationManager
import com.lumera.app.data.trakt.TraktAuthManager
import com.lumera.app.data.trakt.TraktSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.lumera.app.data.profile.ProfileRuntimeCoordinator
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dao: AddonDao,
    private val profileConfigurationManager: ProfileConfigurationManager,
    private val traktAuthManager: TraktAuthManager,
    private val traktSyncManager: TraktSyncManager,
    private val coordinator: ProfileRuntimeCoordinator
) : ViewModel() {

    private val _activeProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile: StateFlow<ProfileEntity?> = _activeProfile

    private val transitionMutex = Mutex()
    private var connectionJob: Job? = null
    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError
    fun clearProfileError() { _profileError.value = null }

    private var profileJob: Job? = null
    private var traktSyncJob: Job? = null
    private var activeProfileId: Int? = null

    companion object {
        private const val TRAKT_POLL_INTERVAL_MS = 30_000L // 30 seconds
    }

    // Call this when user clicks a profile
    fun login(id: Int) {
        viewModelScope.launch {
            transitionMutex.withLock {
                try {
                    connectionJob?.cancelAndJoin()
                    traktSyncJob?.cancelAndJoin()
                    profileJob?.cancelAndJoin()
                    check(dao.getProfileById(id) != null) { "This profile no longer exists." }
                    profileConfigurationManager.switchRuntimeState(id)
                    activeProfileId = id
                    _activeProfile.value = dao.getProfileById(id)
                    profileJob = launch {
                        dao.getProfileFlow(id).collect { _activeProfile.value = it }
                    }
                    traktAuthManager.refreshConnectionState()
                    traktSyncManager.resetActivityState()
                    connectionJob = launch {
                        traktAuthManager.isConnected.collect { connected ->
                            if (connected && traktSyncJob?.isActive != true) startTraktPeriodicSync()
                            else if (!connected) traktSyncJob?.cancelAndJoin()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _profileError.value = error.message ?: "Unable to open this profile. Your saved data has been preserved."
                }
            }
        }
    }

    private fun startTraktPeriodicSync() {
        traktSyncJob?.cancel()
        if (!traktAuthManager.isConnected.value) return

        val owner = coordinator.session
        traktSyncJob = viewModelScope.launch(Dispatchers.IO) {
            // Immediate full sync on login
            coordinator.forSession(owner) {
                traktSyncManager.syncWatchlist()
                traktSyncManager.syncPlaybackProgress()
                traktSyncManager.syncSeriesNextUp()
            }

            // Then lightweight activity check every 30 seconds —
            // only triggers a full sync when Trakt detects changes
            while (isActive) {
                delay(TRAKT_POLL_INTERVAL_MS)
                coordinator.forSession(owner) { traktSyncManager.checkAndSync() }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            transitionMutex.withLock {
                try {
                    connectionJob?.cancelAndJoin()
                    traktSyncJob?.cancelAndJoin()
                    profileJob?.cancelAndJoin()
                    profileConfigurationManager.endSession()
                    activeProfileId = null
                    _activeProfile.value = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _profileError.value = "Unable to save this profile. Please try again."
                }
            }
        }
    }

    suspend fun persistActiveProfileState() {
        profileConfigurationManager.saveActiveRuntimeState()
    }
}
