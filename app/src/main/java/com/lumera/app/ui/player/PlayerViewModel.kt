package com.lumera.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.WatchHistoryEntity
import com.lumera.app.data.trakt.TraktScrobbleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lumera.app.data.profile.ProfileRuntimeCoordinator
import com.lumera.app.data.profile.ProfileSession
import com.lumera.app.data.player.PlaybackCompletion
import javax.inject.Inject


@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val dao: AddonDao,
    private val traktScrobbleManager: TraktScrobbleManager,
    private val coordinator: ProfileRuntimeCoordinator
) : ViewModel() {

    fun captureSession(): ProfileSession? = coordinator.session

    fun saveProgress(
        id: String,
        type: String,
        title: String,
        poster: String?,
        position: Long,
        duration: Long?,
        owner: ProfileSession?,
        thresholdPercent: Int
    ) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            if (id.startsWith("trailer_")) return@launch
            val safePosition = position.coerceAtLeast(0L)
            if (safePosition < 5_000L) return@launch

            coordinator.forSession(owner) {
                val existing = dao.getHistoryItem(id)
                val safeDuration = duration?.takeIf { it > 0L }
                    ?: existing?.duration?.takeIf { it > 0L } ?: 0L
                val isCompleted = PlaybackCompletion.isCompleted(safePosition, safeDuration, thresholdPercent)

                val entry = WatchHistoryEntity(
                    id = id,
                    title = title,
                    poster = poster ?: existing?.poster,
                    background = existing?.background,
                    logo = existing?.logo,
                    position = safePosition,
                    duration = safeDuration,
                    lastWatched = System.currentTimeMillis(),
                    type = type.ifBlank { "movie" },
                    watched = isCompleted,
                    scrobbled = existing?.scrobbled ?: traktScrobbleManager.isScrobbled(id)
                )
                dao.upsertHistory(entry)
            }
        }
    }

    // ── Trakt Scrobbling ──

    fun scrobbleStart(id: String, type: String, positionMs: Long, durationMs: Long, owner: ProfileSession?) {
        viewModelScope.launch(Dispatchers.IO) {
            coordinator.forSession(owner) { traktScrobbleManager.scrobbleStart(id, type, positionMs, durationMs) }
        }
    }

    fun scrobblePause(id: String, type: String, positionMs: Long, durationMs: Long, force: Boolean = false, owner: ProfileSession?) {
        viewModelScope.launch(Dispatchers.IO) {
            coordinator.forSession(owner) { traktScrobbleManager.scrobblePause(id, type, positionMs, durationMs, force = force) }
        }
    }

    fun scrobbleStop(id: String, type: String, positionMs: Long, durationMs: Long, owner: ProfileSession?) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            coordinator.forSession(owner) { traktScrobbleManager.scrobbleStop(id, type, positionMs, durationMs) }
        }
    }

    suspend fun getResumePosition(id: String, owner: ProfileSession?): Long {
        return withContext(Dispatchers.IO) {
            coordinator.forSession(owner) {
                dao.getHistoryItem(id)?.position?.takeIf { it > 0 } ?: 0L
            } ?: 0L
        }
    }
}
