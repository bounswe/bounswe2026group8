package com.bounswe2026group8.emergencyhub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.RepostRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.VoteRequest
import com.bounswe2026group8.emergencyhub.util.ErrorUtils
import kotlinx.coroutines.launch

class ForumViewModel(app: Application) : AndroidViewModel(app) {

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** Event: post was deleted, notify UI to remove it. */
    private val _postDeleted = MutableLiveData<Int?>()
    val postDeleted: LiveData<Int?> = _postDeleted

    /** Event: repost succeeded, reload needed. */
    private val _repostResult = MutableLiveData<String?>()
    val repostResult: LiveData<String?> = _repostResult

    fun loadPosts(forumType: String, hubId: Int?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .getPosts(forumType = forumType, hub = hubId)
                if (response.isSuccessful) {
                    _posts.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load posts"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun vote(postId: Int, voteType: String) {
        val currentPosts = _posts.value?.toMutableList() ?: return
        val idx = currentPosts.indexOfFirst { it.id == postId }
        if (idx == -1) return
        val post = currentPosts[idx]
        val original = post

        var newUp = post.upvoteCount
        var newDown = post.downvoteCount
        var newUserVote: String? = voteType

        if (post.userVote == voteType) {
            if (voteType == "UP") newUp-- else newDown--
            newUserVote = null
        } else if (post.userVote != null) {
            if (post.userVote == "UP") { newUp--; newDown++ }
            else { newDown--; newUp++ }
        } else {
            if (voteType == "UP") newUp++ else newDown++
        }

        val updated = post.copy(upvoteCount = newUp, downvoteCount = newDown, userVote = newUserVote)
        currentPosts[idx] = updated
        _posts.value = currentPosts

        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .vote(postId, VoteRequest(voteType))
                if (!response.isSuccessful) {
                    // Revert
                    val reverted = _posts.value?.toMutableList() ?: return@launch
                    val i = reverted.indexOfFirst { it.id == postId }
                    if (i != -1) { reverted[i] = original; _posts.value = reverted }
                }
            } catch (_: Exception) {
                val reverted = _posts.value?.toMutableList() ?: return@launch
                val i = reverted.indexOfFirst { it.id == postId }
                if (i != -1) { reverted[i] = original; _posts.value = reverted }
            }
        }
    }

    fun repost(postId: Int, hubId: Int?) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .repost(postId, RepostRequest(hub = hubId))
                if (response.isSuccessful) {
                    _repostResult.value = "Reposted!"
                } else {
                    val errorBody = response.errorBody()?.string()
                    _repostResult.value = ErrorUtils.parseApiError(errorBody, "Could not repost.")
                }
            } catch (_: Exception) {
                _repostResult.value = "Network error"
            }
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication()).deletePost(postId)
                if (response.isSuccessful || response.code() == 204) {
                    _posts.value = _posts.value?.filter { it.id != postId }
                    _postDeleted.value = postId
                } else {
                    _error.value = "Failed to delete post"
                }
            } catch (_: Exception) {
                _error.value = "Network error"
            }
        }
    }

    fun removePost(postId: Int) {
        _posts.value = _posts.value?.filter { it.id != postId }
    }

    fun clearError() { _error.value = null }
    fun clearRepostResult() { _repostResult.value = null }
    fun clearPostDeleted() { _postDeleted.value = null }
}
