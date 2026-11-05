package com.lumera.app.ui.splash

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class CinevoSplashViewTest {
    @Test fun readyAppKeepsBriefIntroThenFinishesExactlyOnce() = checkCompletion(true, 1_600L)
    @Test fun slowStartupCannotKeepOverlayIndefinitely() = checkCompletion(false, 6_000L)

    private fun checkCompletion(ready: Boolean, hold: Long) {
        val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val activity = controller.get()
        var now = 0L
        var finished = 0
        val splash = CinevoSplashView(activity, { ready }, { finished++ }, { now })
        activity.setContentView(splash)
        splash.layout(0, 0, 960, 540)
        val bitmap = Bitmap.createBitmap(960, 540, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        now = hold - 1
        splash.draw(canvas)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, finished)
        now = hold
        splash.draw(canvas)
        now = hold + 281
        splash.draw(canvas)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, finished)
        splash.draw(canvas)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, finished)
        activity.setContentView(View(activity))
        bitmap.recycle()
        controller.pause().stop().destroy()
    }
}
