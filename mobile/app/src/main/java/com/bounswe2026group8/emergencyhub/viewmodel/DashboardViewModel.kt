package com.bounswe2026group8.emergencyhub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bounswe2026group8.emergencyhub.api.FcmTokenRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.UserData
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    private val _user = MutableLiveData<UserData?>()
    val user: LiveData<UserData?> = _user

    private val _navigateToLanding = MutableLiveData(false)
    val navigateToLanding: LiveData<Boolean> = _navigateToLanding

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun loadUser() {
        // Show cached user immediately
        _user.value = tokenManager.getUser()
        // Then refresh from backend
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication()).getMe()
                if (response.isSuccessful) {
                    val userData = response.body()!!
                    tokenManager.saveUser(userData)
                    _user.value = userData
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    _navigateToLanding.value = true
                }
            } catch (_: Exception) { }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                RetrofitClient.getService(getApplication()).logout()
            } catch (_: Exception) { }
            tokenManager.clear()
            _navigateToLanding.value = true
        }
    }

    fun sendFcmToken() {
        viewModelScope.launch {
            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                RetrofitClient.getService(getApplication())
                    .updateFcmToken(FcmTokenRequest(fcmToken))
            } catch (_: Exception) { }
        }
    }
}
