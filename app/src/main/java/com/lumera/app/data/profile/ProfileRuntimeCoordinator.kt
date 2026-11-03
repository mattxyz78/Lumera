package com.lumera.app.data.profile

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** A token belongs to one activation, including when the same profile is reopened. */
data class ProfileSession(val profileId: Int, val generation: Long)

@Singleton
class ProfileRuntimeCoordinator @Inject constructor() {
    private val mutex = Mutex()
    private var generation = 0L
    @Volatile var session: ProfileSession? = null
        private set

    suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }

    // Only call while holding exclusive().
    fun activate(profileId: Int) {
        session = ProfileSession(profileId, ++generation)
    }

    fun deactivate() {
        session = null
        generation++
    }

    suspend fun <T> forSession(owner: ProfileSession?, block: suspend () -> T): T? =
        mutex.withLock {
            if (owner == null || owner != session) null else block()
        }
}
