package com.lumera.app.ui.components

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class, qualifiers = "w360dp-h640dp-port-notnight")
class DeviceSurfaceTouchTest {
    @Test fun handheldCardRespondsToPhysicalTapExactlyOnce() = checkTap(true)
    @Test fun disabledHandheldCardRejectsPhysicalTap() = checkTap(false)

    private fun checkTap(enabled: Boolean) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().visible()
        val activity = controller.get()
        var clicks = 0
        var center: Offset? = null
        activity.setContent {
            DeviceSurface(onClick = { clicks++ }, enabled = enabled,
                modifier = Modifier.size(100.dp).onGloballyPositioned { center = it.boundsInWindow().center }) {
                Text("Content")
            }
        }
        val decor = activity.window.decorView
        repeat(5) {
            decor.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY))
            decor.layout(0, 0, 360, 640)
            shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(32))
        }
        assertNotNull("Card must have a laid-out touch target", center)
        val point = center!!
        val now = SystemClock.uptimeMillis()
        fun dispatch(action: Int, time: Long) {
            MotionEvent.obtain(now, time, action, point.x, point.y, 0).also {
                decor.dispatchTouchEvent(it)
                it.recycle()
            }
        }
        dispatch(MotionEvent.ACTION_DOWN, now)
        dispatch(MotionEvent.ACTION_UP, now + 50)
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertEquals(if (enabled) 1 else 0, clicks)
        controller.pause().stop().destroy()
    }
}
