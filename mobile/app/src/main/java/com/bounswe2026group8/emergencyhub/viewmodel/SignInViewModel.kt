package com.bounswe2026group8.emergencyhub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bounswe2026group8.emergencyhub.api.LoginRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import kotlinx.coroutines.launch

class SignInViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication()).login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenManager.saveToken(body.token!!, body.refresh)
                    body.user?.let { tokenManager.saveUser(it) }
                    _loginResult.value = LoginResult.Success
                } else {
                    _loginResult.value = LoginResult.Error("Invalid email or password")
                }
            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error("Network error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class LoginResult {
        object Success : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}
