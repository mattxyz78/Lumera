package com.lumera.app.data.profile

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProfileRuntimeCoordinatorTest {
    @Test fun lateSaveCannotWriteIntoNewProfile() = runTest {
        val coordinator = ProfileRuntimeCoordinator()
        coordinator.exclusive { coordinator.activate(1) }
        val owner = coordinator.session
        val results = mutableListOf<String>()
        coordinator.exclusive { coordinator.activate(2) }
        coordinator.forSession(owner) { results.add("old playback") }
        coordinator.forSession(coordinator.session) { results.add("current playback") }
        assertEquals(listOf("current playback"), results)
    }

    @Test fun reopeningSameProfileInvalidatesOldSession() = runTest {
        val coordinator = ProfileRuntimeCoordinator()
        coordinator.exclusive { coordinator.activate(1) }
        val old = coordinator.session
        coordinator.exclusive { coordinator.deactivate(); coordinator.activate(1) }
        assertNull(coordinator.forSession(old) { "must not run" })
        assertNull(coordinator.forSession(null) { "must not run" })
    }

    @Test fun switchWaitsForAcceptedWriteToFinish() = runTest {
        val coordinator = ProfileRuntimeCoordinator()
        coordinator.exclusive { coordinator.activate(1) }
        val owner = coordinator.session
        val release = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()
        val writer = launch {
            coordinator.forSession(owner) {
                order.add("write started")
                release.await()
                order.add("write finished")
            }
        }
        runCurrent()
        val switcher = launch { coordinator.exclusive { coordinator.activate(2); order.add("switched") } }
        runCurrent()
        assertEquals(listOf("write started"), order)
        release.complete(Unit)
        writer.join(); switcher.join()
        assertEquals(listOf("write started", "write finished", "switched"), order)
    }
}
