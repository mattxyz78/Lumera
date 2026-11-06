package com.lumera.app.ui.parental

import androidx.lifecycle.ViewModel
import com.lumera.app.data.model.stremio.MetaItem
import com.lumera.app.data.parental.ParentalAccess
import com.lumera.app.data.profile.ProfileRuntimeCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContentPolicyViewModel @Inject constructor(private val access: ParentalAccess, private val coordinator: ProfileRuntimeCoordinator) : ViewModel() {
    fun sessionKey() = coordinator.session
    suspend fun allowed(type: String, id: String) = access.allowed(type, id)
    suspend fun filter(items: List<MetaItem>) = access.filter(items)
}
