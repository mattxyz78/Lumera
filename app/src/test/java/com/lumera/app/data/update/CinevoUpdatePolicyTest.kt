package com.lumera.app.data.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class CinevoUpdatePolicyTest {
    @Test fun startupAndDirectInstallCannotFetchUpstreamApk() = runTest {
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor {
            requests++
            error("Updater must not use the network")
        }.build()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AppUpdateManager(client, context)
        manager.setPopupEnabled(true)
        assertFalse(manager.isPopupEnabled)
        manager.checkForUpdate()
        assertTrue(manager.state.value is UpdateState.Error)
        manager.downloadAndInstall("https://github.com/LumeraD3v/Lumera/releases/download/test/app.apk")
        assertTrue(manager.state.value is UpdateState.Error)
        assertEquals(0, requests)
    }
}
