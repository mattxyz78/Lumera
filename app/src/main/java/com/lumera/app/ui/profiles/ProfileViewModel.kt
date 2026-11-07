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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
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
    private val sourceSelections: SourceSelectionStore,
    private val parentPin: com.lumera.app.data.parental.ParentPin
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
    var tempAgeGroup: String? = null
    var tempPinProtected = false
    private var tempRatingKey = ""
    val hasRatingKey get() = parentPin.ratingApiKey.isNotBlank()
    var tempName = ""
    var tempAvatarRef = "avatar_1"
    var tempThemeId = "void"  // Changed from tempColor

    private var editingProfileId: Int? = null
    private var wizardAuthorized = false
    private var wizardSaveJob: Job? = null

    private suspend fun profileChangesRequirePin() = dao.getProfiles().first().any { it.pinProtected }

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
        viewModelScope.launch {
            val needsPin = profileChangesRequirePin()
            if (needsPin && !parentPin.authorize("Create a profile")) return@launch
            wizardAuthorized = needsPin
            startWizardAuthorized()
        }
    }

    private fun startWizardAuthorized() {
        editingProfileId = null
        tempAgeGroup = null
        tempPinProtected = false
        tempRatingKey = ""
        tempName = ""
        tempAvatarRef = "avatar_1"
        tempThemeId = "void"
        _wizardStep.value = 1
    }

    fun startEditWizard(profile: ProfileEntity) {
        viewModelScope.launch {
            val needsPin = profileChangesRequirePin()
            if (needsPin && !parentPin.authorize("Edit profile and age group")) return@launch
            wizardAuthorized = needsPin
            startEditWizardAuthorized(profile)
        }
    }

    private fun startEditWizardAuthorized(profile: ProfileEntity) {
        editingProfileId = profile.id
        tempAgeGroup = profile.ageGroup
        tempPinProtected = profile.pinProtected
        tempRatingKey = ""
        tempName = profile.name
        tempAvatarRef = profile.avatarRef
        tempThemeId = profile.themeId
        _wizardStep.value = 1
    }

    fun cancelWizard() {
        wizardSaveJob?.cancel()
        wizardAuthorized = false
        tempRatingKey = ""
        _wizardStep.value = 0
        editingProfileId = null
    }

    fun setWizardName(name: String) {
        tempName = name
        _wizardStep.value = 2
    }

    fun setWizardAgeGroup(ageGroup: String, pinProtected: Boolean = tempPinProtected, ratingKey: String = tempRatingKey) {
        require(ageGroup in listOf("under12", "12to14", "15to17", "adult"))
        tempAgeGroup = ageGroup
        tempPinProtected = pinProtected
        tempRatingKey = ratingKey
        _wizardStep.value = 3
    }

    fun setWizardAvatar(avatarKey: String) {
        tempAvatarRef = avatarKey
        _wizardStep.value = 4
    }

    fun setWizardTheme(themeId: String) {
        tempThemeId = themeId
        finishWizard()
    }

    private fun finishWizard() {
        val selectedAgeGroup = tempAgeGroup ?: return
        if (wizardSaveJob?.isActive == true) return
        val editingId = editingProfileId
        val pinProtected = tempPinProtected
        val name = tempName
        val avatar = tempAvatarRef
        val theme = tempThemeId
        val ratingKey = tempRatingKey
        wizardSaveJob = viewModelScope.launch {
            // Set or verify the parent PIN before saving the first optional lock.
            if ((pinProtected || profileChangesRequirePin()) && !wizardAuthorized &&
                !parentPin.authorize("Save profile PIN protection")) return@launch
            if (ratingKey.isNotBlank() && !parentPin.saveRatingApiKey(ratingKey)) {
                _operationError.value = "Unable to save the ratings key. Please try again."
                return@launch
            }
            withContext(NonCancellable) {
                if (editingId != null) {
                    val updatedProfile = dao.getProfileById(editingId)?.copy(
                        ageGroup = selectedAgeGroup,
                        pinProtected = pinProtected,
                        name = name,
                        avatarRef = avatar,
                        themeId = theme
                    )
                    if (updatedProfile != null) dao.updateProfile(updatedProfile)
                } else {
                    val profileId = dao.insertProfile(
                        ProfileEntity(
                            ageGroup = selectedAgeGroup,
                            pinProtected = pinProtected,
                            name = name,
                            avatarRef = avatar,
                            themeId = theme,
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
                wizardAuthorized = false
                tempRatingKey = ""
            }
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
            if (profileChangesRequirePin() && !parentPin.authorize("Delete profile")) return@launch
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
