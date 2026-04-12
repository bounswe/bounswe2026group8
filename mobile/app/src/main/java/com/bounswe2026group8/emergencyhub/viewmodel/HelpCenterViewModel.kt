package com.bounswe2026group8.emergencyhub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for HelpRequestListActivity and HelpOfferListActivity.
 * Manages fetching, filtering, and deleting help requests and offers.
 */
class HelpCenterViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    private val _requests = MutableLiveData<List<HelpRequestItem>>(emptyList())
    val requests: LiveData<List<HelpRequestItem>> = _requests

    private val _offers = MutableLiveData<List<HelpOfferItem>>(emptyList())
    val offers: LiveData<List<HelpOfferItem>> = _offers

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _navigateToLanding = MutableLiveData(false)
    val navigateToLanding: LiveData<Boolean> = _navigateToLanding

    private val _offerDeletedMessage = MutableLiveData<String?>()
    val offerDeletedMessage: LiveData<String?> = _offerDeletedMessage

    fun fetchHelpRequests(hubId: Int?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .getHelpRequests(hubId = hubId)
                if (response.isSuccessful) {
                    _requests.value = response.body() ?: emptyList()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    _navigateToLanding.value = true
                } else {
                    _error.value = "Failed to load help requests"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchHelpOffers(hubId: Int?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .getHelpOffers(hubId = hubId)
                if (response.isSuccessful) {
                    _offers.value = response.body() ?: emptyList()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    _navigateToLanding.value = true
                } else {
                    _error.value = "Failed to load help offers"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteOffer(offerId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .deleteHelpOffer(offerId)
                if (response.code() == 204 || response.isSuccessful) {
                    _offers.value = _offers.value?.filter { it.id != offerId }
                    _offerDeletedMessage.value = "deleted"
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    _navigateToLanding.value = true
                } else {
                    _error.value = "Failed to delete offer"
                }
            } catch (e: Exception) {
                if (e.message?.contains("HTTP 204") == true) {
                    _offers.value = _offers.value?.filter { it.id != offerId }
                    _offerDeletedMessage.value = "deleted"
                } else {
                    _error.value = "Network error: ${e.localizedMessage}"
                }
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearOfferDeletedMessage() { _offerDeletedMessage.value = null }
}
