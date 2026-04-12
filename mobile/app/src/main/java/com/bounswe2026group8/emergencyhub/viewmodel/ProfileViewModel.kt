package com.bounswe2026group8.emergencyhub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bounswe2026group8.emergencyhub.api.ExpertiseFieldCreateRequest
import com.bounswe2026group8.emergencyhub.api.ExpertiseFieldData
import com.bounswe2026group8.emergencyhub.api.ProfileData
import com.bounswe2026group8.emergencyhub.api.ProfileUpdateRequest
import com.bounswe2026group8.emergencyhub.api.ResourceCreateRequest
import com.bounswe2026group8.emergencyhub.api.ResourceData
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val _profile = MutableLiveData<ProfileData?>()
    val profile: LiveData<ProfileData?> = _profile

    private val _resources = MutableLiveData<List<ResourceData>>(emptyList())
    val resources: LiveData<List<ResourceData>> = _resources

    private val _expertise = MutableLiveData<List<ExpertiseFieldData>>(emptyList())
    val expertise: LiveData<List<ExpertiseFieldData>> = _expertise

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.getService(getApplication()).getProfile()
                if (res.isSuccessful) _profile.value = res.body()
            } catch (_: Exception) { }
        }
    }

    fun updateProfile(request: ProfileUpdateRequest) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.getService(getApplication()).updateProfile(request)
                if (res.isSuccessful) {
                    _profile.value = res.body()
                    _message.value = "Saved"
                } else {
                    _message.value = "Update failed"
                }
            } catch (_: Exception) {
                _message.value = "Network error"
            }
        }
    }

    fun loadResources() {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.getService(getApplication()).getResources()
                if (res.isSuccessful) _resources.value = res.body() ?: emptyList()
            } catch (_: Exception) { }
        }
    }

    fun createResource(request: ResourceCreateRequest) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.getService(getApplication()).createResource(request)
                if (res.isSuccessful) {
                    _message.value = "Resource added"
                    loadResources()
                } else {
                    _message.value = "Failed to add resource"
                }
            } catch (_: Exception) {
                _message.value = "Network error"
            }
        }
    }

    fun deleteResource(id: Int) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.getService(getApplication()).deleteResource(id)
                if (res.isSuccessful) {
                    _message.value = "Removed"
                    loadResources()
                }
            } catch (_: Exception) { }
        }
    }

    fun loadExpertise() {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.getService(getApplication()).getExpertiseFields()
                if (res.isSuccessful) _expertise.value = res.body() ?: emptyList()
            } catch (_: Exception) { }
        }
    }

    fun createExpertise(request: ExpertiseFieldCreateRequest) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.getService(getApplication()).createExpertiseField(request)
                if (res.isSuccessful) {
                    _message.value = "Expertise added"
                    loadExpertise()
                } else {
                    _message.value = "Failed to add expertise"
                }
            } catch (_: Exception) {
                _message.value = "Network error"
            }
        }
    }

    fun deleteExpertise(id: Int) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.getService(getApplication()).deleteExpertiseField(id)
                if (res.isSuccessful) {
                    _message.value = "Removed"
                    loadExpertise()
                }
            } catch (_: Exception) { }
        }
    }

    fun clearMessage() { _message.value = null }
}
