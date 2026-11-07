package com.lumera.app.data.parental

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ParentPinTest {
    private lateinit var context: Context
    private lateinit var pin: ParentPin
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("cinevo_parent_pin", 0).edit().clear().commit()
        pin = ParentPin(context)
    }
    @Test fun setupRequiresConfirmationAndStoresNoPlainPin() = runTest {
        val result = async { pin.authorize("Create profile") }; runCurrent()
        pin.submit("5291", "1234"); assertFalse(result.isCompleted)
        pin.submit("5291", "5291"); assertTrue(result.await())
        assertTrue(pin.configured)
        assertFalse(context.getSharedPreferences("cinevo_parent_pin", 0).all.values.any { it == "5291" })
    }
    @Test fun ratingsSetupDoesNotCreateOrEnableAPin() {
        assertFalse(pin.configured)
        assertTrue(pin.saveRatingApiKey("ratings-key"))
        assertEquals("ratings-key", pin.ratingApiKey)
        assertEquals(1, pin.ratingKeyVersion)
        assertFalse(pin.configured)
    }
    @Test fun wrongPinAndCancelledPromptsDoNotAuthorize() = runTest {
        val setup = async { pin.authorize("Setup") }; runCurrent(); pin.submit("5291", "5291"); assertTrue(setup.await())
        val result = async { pin.authorize("Adult profile") }; runCurrent()
        pin.submit("0000", ""); assertFalse(result.isCompleted)
        pin.cancel(); assertFalse(result.await())
        val next = async { pin.authorize("Adult profile") }; runCurrent()
        pin.submit("5291", ""); assertTrue(next.await())
    }
    @Test fun identicalPromptReasonsStillGetUniqueRequests() = runTest {
        val first = async { pin.authorize("Edit") }; runCurrent(); val id = pin.prompt.value!!.id
        pin.cancel(); assertFalse(first.await())
        val second = async { pin.authorize("Edit") }; runCurrent()
        assertNotEquals(id, pin.prompt.value!!.id)
        pin.cancel(); assertFalse(second.await())
    }
    @Test fun failedAttemptsCannotChangeRatingKeyAndAreThrottled() = runTest {
        val setup = async { pin.authorize("Setup") }; runCurrent()
        pin.submit("5291", "5291", "original-key"); assertTrue(setup.await())
        val result = async { pin.authorize("Edit") }; runCurrent()
        repeat(5) { pin.submit("0000", "", "replacement-key") }
        assertEquals("original-key", pin.ratingApiKey)
        pin.submit("5291", ""); assertFalse(result.isCompleted)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(31))
        pin.submit("5291", "", "replacement-key"); assertTrue(result.await())
        assertEquals("replacement-key", pin.ratingApiKey)
        assertEquals(2, pin.ratingKeyVersion)
    }
}
