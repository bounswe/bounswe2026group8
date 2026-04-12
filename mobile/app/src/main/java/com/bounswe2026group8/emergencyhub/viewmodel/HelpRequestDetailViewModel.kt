package com.bounswe2026group8.emergencyhub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bounswe2026group8.emergencyhub.api.CreateCommentRequest
import com.bounswe2026group8.emergencyhub.api.HelpRequestComment
import com.bounswe2026group8.emergencyhub.api.HelpRequestDetail
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.UpdateHelpRequestStatusRequest
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import kotlinx.coroutines.launch

class HelpRequestDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    private val _detail = MutableLiveData<HelpRequestDetail?>()
    val detail: LiveData<HelpRequestDetail?> = _detail

    private val _comments = MutableLiveData<List<HelpRequestComment>>(emptyList())
    val comments: LiveData<List<HelpRequestComment>> = _comments

    private val _commentsLoading = MutableLiveData(false)
    val commentsLoading: LiveData<Boolean> = _commentsLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _navigateToLanding = MutableLiveData(false)
    val navigateToLanding: LiveData<Boolean> = _navigateToLanding

    private val _requestDeleted = MutableLiveData(false)
    val requestDeleted: LiveData<Boolean> = _requestDeleted

    fun fetchDetail(requestId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .getHelpRequestDetail(requestId)
                if (response.isSuccessful) {
                    _detail.value = response.body()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    _navigateToLanding.value = true
                } else {
                    _message.value = "Failed to load details"
                }
            } catch (e: Exception) {
                _message.value = "Network error: ${e.localizedMessage}"
            }
        }
    }

    fun fetchComments(requestId: Int) {
        _commentsLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .getHelpRequestComments(requestId)
                if (response.isSuccessful) {
                    _comments.value = response.body() ?: emptyList()
                } else {
                    _message.value = "Failed to load comments"
                }
            } catch (e: Exception) {
                _message.value = "Network error: ${e.localizedMessage}"
            } finally {
                _commentsLoading.value = false
            }
        }
    }

    fun submitComment(requestId: Int, content: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .createHelpRequestComment(requestId, CreateCommentRequest(content))
                if (response.isSuccessful) {
                    val comment = response.body()!!
                    _comments.value = _comments.value.orEmpty() + comment
                    // Re-fetch detail to update comment count and status
                    fetchDetail(requestId)
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    _navigateToLanding.value = true
                } else {
                    val errorText = response.errorBody()?.string() ?: "Failed to post comment."
                    _message.value = errorText
                }
            } catch (e: Exception) {
                _message.value = "Network error: ${e.localizedMessage}"
            }
        }
    }

    fun deleteComment(requestId: Int, commentId: Int) {
        // Optimistic removal
        _comments.value = _comments.value?.filter { it.id != commentId }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .deleteHelpRequestComment(commentId)
                if (response.isSuccessful || response.code() == 204) {
                    fetchDetail(requestId)
                } else {
                    fetchComments(requestId)
                    _message.value = "Failed to delete comment."
                }
            } catch (_: Exception) {
                fetchComments(requestId)
            }
        }
    }

    fun resolveRequest(requestId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .updateHelpRequestStatus(requestId, UpdateHelpRequestStatusRequest("RESOLVED"))
                if (response.isSuccessful) {
                    _detail.value = response.body()
                    _message.value = "Marked as resolved."
                } else {
                    _message.value = "Failed to update status."
                }
            } catch (_: Exception) {
                _message.value = "Network error."
            }
        }
    }

    fun deleteRequest(requestId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .deleteHelpRequest(requestId)
                if (response.code() == 403) {
                    _message.value = "You can only delete your own requests."
                    return@launch
                }
            } catch (_: Exception) { }
            _requestDeleted.value = true
        }
    }

    fun clearMessage() { _message.value = null }
}
