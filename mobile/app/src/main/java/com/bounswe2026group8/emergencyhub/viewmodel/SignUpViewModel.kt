package com.bounswe2026group8.emergencyhub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bounswe2026group8.emergencyhub.api.Hub
import com.bounswe2026group8.emergencyhub.api.RegisterRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import kotlinx.coroutines.launch

class SignUpViewModel(app: Application) : AndroidViewModel(app) {

    private val _hubs = MutableLiveData<List<Hub>>(emptyList())
    val hubs: LiveData<List<Hub>> = _hubs

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadHubs() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication()).getHubs()
                if (response.isSuccessful) {
                    _hubs.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) { }
        }
    }

    fun register(request: RegisterRequest) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication()).register(request)
                if (response.isSuccessful) {
                    _registerResult.value = RegisterResult.Success
                } else {
                    val errorBody = response.body()
                    val errorMsg = errorBody?.errors?.values?.flatten()?.joinToString("\n")
                        ?: errorBody?.message
                        ?: "Registration failed."
                    _registerResult.value = RegisterResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                _registerResult.value = RegisterResult.Error("Network error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class RegisterResult {
        object Success : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }
}
