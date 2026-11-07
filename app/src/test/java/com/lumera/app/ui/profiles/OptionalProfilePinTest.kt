package com.lumera.app.ui.profiles

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.parental.ParentPin
import com.lumera.app.data.player.PlaybackTrackSelectionStore
import com.lumera.app.data.player.SourceSelectionStore
import com.lumera.app.data.profile.ProfileConfigurationManager
import com.lumera.app.data.profile.ProfileRuntimeCoordinator
import com.lumera.app.data.trakt.TraktAuthManager
import com.lumera.app.data.trakt.TraktSyncManager
import com.lumera.app.ui.MainViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OptionalProfilePinTest {
    private val dao = mockk<AddonDao>(relaxed = true)
    private val manager = mockk<ProfileConfigurationManager>(relaxed = true)
    private val trakt = mockk<TraktAuthManager>(relaxed = true)
    private val pin = mockk<ParentPin>(relaxed = true)
    private val profiles = MutableStateFlow(listOf(ProfileEntity(id = 1, name = "Child", ageGroup = "under12")))
    private val scopes = mutableListOf<CoroutineScope>()

    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { dao.getProfiles() } returns profiles
        coEvery { dao.getProfileById(any()) } coAnswers { profiles.value.find { it.id == firstArg<Int>() } }
        every { dao.getProfileFlow(any()) } answers {
            val id = firstArg<Int>(); profiles.map { it.find { p -> p.id == id } }.filterNotNull()
        }
        every { trakt.isConnected } returns MutableStateFlow(false)
        coEvery { pin.authorize(any()) } returns true
    }
    @After fun cleanup() { scopes.forEach { it.cancel() }; Dispatchers.resetMain() }

    private fun wizard() = ProfileViewModel(dao, manager, trakt,
        mockk<PlaybackTrackSelectionStore>(relaxed = true), mockk<SourceSelectionStore>(relaxed = true), pin)
        .also { scopes += it.viewModelScope }

    private fun main() = MainViewModel(dao, manager, trakt, mockk<TraktSyncManager>(relaxed = true),
        ProfileRuntimeCoordinator(), pin).also { scopes += it.viewModelScope }

    @Test fun unlockedChildAndAdultProfilesOpenWithoutPin() = runTest {
        val vm = main()
        for (age in listOf("under12", "adult")) {
            profiles.value = listOf(ProfileEntity(id = 1, name = "Profile", ageGroup = age))
            vm.login(1); runCurrent()
            assertEquals(age, vm.activeProfile.value?.ageGroup)
        }
        coVerify(exactly = 0) { pin.authorize(any()) }
        coVerify(exactly = 2) { manager.switchRuntimeState(1) }
    }
    @Test fun cancelledPinCannotOpenLockedProfile() = runTest {
        profiles.value = listOf(ProfileEntity(id = 1, name = "Adult", pinProtected = true))
        coEvery { pin.authorize(any()) } returns false
        val vm = main(); vm.login(1); runCurrent()
        assertNull(vm.activeProfile.value)
        coVerify(exactly = 0) { manager.switchRuntimeState(any()) }
    }
    @Test fun correctPinOpensLockedProfile() = runTest {
        profiles.value = listOf(ProfileEntity(id = 1, name = "Adult", pinProtected = true))
        val vm = main(); vm.login(1); runCurrent()
        assertEquals(1, vm.activeProfile.value?.id)
        coVerify(exactly = 1) { pin.authorize(any()) }
    }
    @Test fun noLocksMeansProfileCreationAndEditingNeedNoPin() = runTest {
        val vm = wizard(); runCurrent()
        vm.startWizard(); runCurrent(); assertEquals(1, vm.wizardStep.value)
        vm.cancelWizard(); vm.startEditWizard(profiles.value[0]); runCurrent()
        assertEquals(1, vm.wizardStep.value)
        coVerify(exactly = 0) { pin.authorize(any()) }
    }
    @Test fun savingUnlockedChildRetainsAgeWithoutPin() = runTest {
        val vm = wizard(); runCurrent(); vm.startEditWizard(profiles.value[0]); runCurrent()
        vm.setWizardName("Child"); vm.setWizardAgeGroup("under12", false)
        vm.setWizardAvatar("avatar_1"); vm.setWizardTheme("mattflix"); runCurrent()
        coVerify { dao.updateProfile(match { it.ageGroup == "under12" && !it.pinProtected }) }
        coVerify(exactly = 0) { pin.authorize(any()) }
    }
    @Test fun enablingFirstLockRequiresPinBeforeSaving() = runTest {
        val vm = wizard(); runCurrent(); vm.startEditWizard(profiles.value[0]); runCurrent()
        vm.setWizardName("Child"); vm.setWizardAgeGroup("under12", true)
        vm.setWizardAvatar("avatar_1"); vm.setWizardTheme("mattflix"); runCurrent()
        coVerify(exactly = 1) { pin.authorize(any()) }
        coVerify { dao.updateProfile(match { it.pinProtected && it.ageGroup == "under12" }) }
    }
    @Test fun declinedFirstLockSavesNeitherProfileNorRatingKey() = runTest {
        coEvery { pin.authorize(any()) } returns false
        val vm = wizard(); runCurrent(); vm.startEditWizard(profiles.value[0]); runCurrent()
        vm.setWizardName("Child"); vm.setWizardAgeGroup("under12", true, "new-key")
        vm.setWizardAvatar("avatar_1"); vm.setWizardTheme("mattflix"); runCurrent()
        coVerify(exactly = 0) { dao.updateProfile(any()) }
        verify(exactly = 0) { pin.saveRatingApiKey(any()) }
        assertEquals(4, vm.wizardStep.value)
    }
    @Test fun anyExistingLockProtectsAgeChangesAndNewProfileCreation() = runTest {
        profiles.value += ProfileEntity(id = 2, name = "Locked adult", pinProtected = true)
        coEvery { pin.authorize(any()) } returns false
        val vm = wizard(); runCurrent()
        vm.startEditWizard(profiles.value[0]); runCurrent(); assertEquals(0, vm.wizardStep.value)
        vm.startWizard(); runCurrent(); assertEquals(0, vm.wizardStep.value)
    }
    @Test fun removingLastLockRequiresPinOnceAndRetainsAge() = runTest {
        profiles.value = listOf(profiles.value[0].copy(pinProtected = true))
        val vm = wizard(); runCurrent(); vm.startEditWizard(profiles.value[0]); runCurrent()
        vm.setWizardName("Child"); vm.setWizardAgeGroup("under12", false)
        vm.setWizardAvatar("avatar_1"); vm.setWizardTheme("mattflix"); runCurrent()
        coVerify(exactly = 1) { pin.authorize(any()) }
        coVerify { dao.updateProfile(match { !it.pinProtected && it.ageGroup == "under12" }) }
    }
    @Test fun ratingsCanBeConfiguredWithoutEnablingPin() = runTest {
        every { pin.saveRatingApiKey("ratings-key") } returns true
        val vm = wizard(); runCurrent(); vm.startEditWizard(profiles.value[0]); runCurrent()
        vm.setWizardName("Child"); vm.setWizardAgeGroup("under12", false, "ratings-key")
        vm.setWizardAvatar("avatar_1"); vm.setWizardTheme("mattflix"); runCurrent()
        verify { pin.saveRatingApiKey("ratings-key") }
        coVerify(exactly = 0) { pin.authorize(any()) }
        coVerify { dao.updateProfile(match { !it.pinProtected && it.ageGroup == "under12" }) }
    }
}
