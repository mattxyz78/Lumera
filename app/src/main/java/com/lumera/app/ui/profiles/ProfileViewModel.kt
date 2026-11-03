package com.lumera.app.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.profile.ProfileConfigurationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.lumera.app.data.trakt.TraktAuthManager
import com.lumera.app.data.player.PlaybackTrackSelectionStore
import com.lumera.app.data.player.SourceSelectionStore
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val dao: AddonDao,
    private val profileConfigurationManager: ProfileConfigurationManager,
    private val traktAuthManager: TraktAuthManager,
    private val trackSelections: PlaybackTrackSelectionStore,
    private val sourceSelections: SourceSelectionStore
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<ProfileEntity>>(emptyList())
    val profiles: StateFlow<List<ProfileEntity>> = _profiles

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _wizardStep = MutableStateFlow(0)
    val wizardStep: StateFlow<Int> = _wizardStep

    private val _isInitializingProfile = MutableStateFlow(false)
    val isInitializingProfile: StateFlow<Boolean> = _isInitializingProfile

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError
    fun clearOperationError() { _operationError.value = null }

    // WIZARD DATA
    var tempName = ""
    var tempAvatarRef = "avatar_1"
    var tempThemeId = "void"  // Changed from tempColor

    private var editingProfileId: Int? = null

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            dao.getProfiles().collect { list ->
                _profiles.value = list
                _isLoading.value = false
            }
        }
    }

    // --- WIZARD ACTIONS ---

    fun startWizard() {
        editingProfileId = null
        tempName = ""
        tempAvatarRef = "avatar_1"
        tempThemeId = "void"
        _wizardStep.value = 1
    }

    fun startEditWizard(profile: ProfileEntity) {
        editingProfileId = profile.id
        tempName = profile.name
        tempAvatarRef = profile.avatarRef
        tempThemeId = profile.themeId
        _wizardStep.value = 1
    }

    fun cancelWizard() {
        _wizardStep.value = 0
        editingProfileId = null
    }

    fun setWizardName(name: String) {
        tempName = name
        _wizardStep.value = 2
    }

    fun setWizardAvatar(avatarKey: String) {
        tempAvatarRef = avatarKey
        _wizardStep.value = 3
    }

    fun setWizardTheme(themeId: String) {
        tempThemeId = themeId
        finishWizard()
    }

    private fun finishWizard() {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            if (editingProfileId != null) {
                val updatedProfile = _profiles.value.find { it.id == editingProfileId }?.copy(
                    name = tempName,
                    avatarRef = tempAvatarRef,
                    themeId = tempThemeId
                )
                if (updatedProfile != null) dao.updateProfile(updatedProfile)
            } else {
                val profileId = dao.insertProfile(
                    ProfileEntity(
                        name = tempName,
                        avatarRef = tempAvatarRef,
                        themeId = tempThemeId,
                        navPosition = "left",
                        homeTabLayout = "cinematic",
                        roundCorners = true
                    )
                ).toInt()
                if (profileId > 0) {
                    profileConfigurationManager.markPendingSetup(profileId)
                }
            }
            _wizardStep.value = 0
            editingProfileId = null
        }
    }

    fun needsInitialSetup(profileId: Int): Boolean {
        return profileConfigurationManager.needsInitialSetup(profileId)
    }

    fun initializeProfileFromScratch(profileId: Int, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            _isInitializingProfile.value = true
            try {
                profileConfigurationManager.initializeFromScratch(profileId)
                onComplete()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _operationError.value = error.message ?: "Unable to set up this profile. Your saved data has been preserved."
            } finally {
                _isInitializingProfile.value = false
            }
        }
    }

    fun initializeProfileByCopy(targetProfileId: Int, sourceProfileId: Int, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            _isInitializingProfile.value = true
            try {
                profileConfigurationManager.initializeByCopying(targetProfileId, sourceProfileId)
                onComplete()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _operationError.value = error.message ?: "Unable to set up this profile. Your saved data has been preserved."
            } finally {
                _isInitializingProfile.value = false
            }
        }
    }

    fun initializeProfileWithServicesOnly(targetProfileId: Int, sourceProfileId: Int, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            _isInitializingProfile.value = true
            try {
                profileConfigurationManager.initializeWithServicesOnly(targetProfileId, sourceProfileId)
                onComplete()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _operationError.value = error.message ?: "Unable to set up this profile. Your saved data has been preserved."
            } finally {
                _isInitializingProfile.value = false
            }
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            try {
                profileConfigurationManager.deleteProfileState(id)
                traktAuthManager.clearTokensForProfile(id)
                trackSelections.clearProfile(id)
                sourceSelections.clearProfile(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _operationError.value = error.message ?: "Unable to delete this profile."
            }
        }
    }

    fun goBackStep() {
        if (_wizardStep.value > 0) _wizardStep.value -= 1
    }
}
