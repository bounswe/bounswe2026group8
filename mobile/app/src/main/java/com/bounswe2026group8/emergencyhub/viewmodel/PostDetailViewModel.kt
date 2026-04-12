package com.bounswe2026group8.emergencyhub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bounswe2026group8.emergencyhub.api.Comment
import com.bounswe2026group8.emergencyhub.api.CreateCommentRequest
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.ReportRequest
import com.bounswe2026group8.emergencyhub.api.RepostRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.VoteRequest
import com.bounswe2026group8.emergencyhub.auth.HubManager
import com.bounswe2026group8.emergencyhub.util.ErrorUtils
import kotlinx.coroutines.launch

class PostDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> = _comments

    private val _commentCount = MutableLiveData(0)
    val commentCount: LiveData<Int> = _commentCount

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _postDeleted = MutableLiveData(false)
    val postDeleted: LiveData<Boolean> = _postDeleted

    private val _navigateToLanding = MutableLiveData(false)
    val navigateToLanding: LiveData<Boolean> = _navigateToLanding

    fun loadPost(postId: Int) {
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getService(getApplication())
                val postResponse = api.getPost(postId)
                val commentsResponse = api.getComments(postId)

                if (postResponse.isSuccessful) {
                    _post.value = postResponse.body()
                    _commentCount.value = postResponse.body()?.commentCount ?: 0
                } else {
                    _message.value = "Post not found"
                    return@launch
                }

                if (commentsResponse.isSuccessful) {
                    _comments.value = commentsResponse.body() ?: emptyList()
                }
            } catch (e: Exception) {
                _message.value = "Network error: ${e.message}"
            }
        }
    }

    fun vote(postId: Int, voteType: String) {
        val p = _post.value ?: return
        val prev = p

        var newUp = p.upvoteCount
        var newDown = p.downvoteCount
        var newUserVote: String? = voteType

        if (p.userVote == voteType) {
            if (voteType == "UP") newUp-- else newDown--
            newUserVote = null
        } else if (p.userVote != null) {
            if (p.userVote == "UP") { newUp--; newDown++ }
            else { newDown--; newUp++ }
        } else {
            if (voteType == "UP") newUp++ else newDown++
        }

        _post.value = p.copy(upvoteCount = newUp, downvoteCount = newDown, userVote = newUserVote)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .vote(postId, VoteRequest(voteType))
                if (!response.isSuccessful) _post.value = prev
            } catch (_: Exception) {
                _post.value = prev
            }
        }
    }

    fun repost(postId: Int) {
        viewModelScope.launch {
            try {
                val hubId = HubManager(getApplication()).getSelectedHub()?.id
                val response = RetrofitClient.getService(getApplication())
                    .repost(postId, RepostRequest(hub = hubId))
                if (response.isSuccessful) {
                    _post.value = _post.value?.copy(
                        userHasReposted = true,
                        repostCount = (_post.value?.repostCount ?: 0) + 1
                    )
                    _message.value = "Reposted!"
                } else {
                    val errorBody = response.errorBody()?.string()
                    _message.value = ErrorUtils.parseApiError(errorBody, "Could not repost.")
                }
            } catch (_: Exception) {
                _message.value = "Network error"
            }
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication()).deletePost(postId)
                if (response.isSuccessful || response.code() == 204) {
                    _postDeleted.value = true
                } else {
                    _message.value = "Failed to delete post"
                }
            } catch (_: Exception) {
                _message.value = "Network error"
            }
        }
    }

    fun submitComment(postId: Int, content: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .createComment(postId, CreateCommentRequest(content))
                if (response.isSuccessful) {
                    val comment = response.body()!!
                    _comments.value = _comments.value.orEmpty() + comment
                    _commentCount.value = (_commentCount.value ?: 0) + 1
                } else {
                    _message.value = "Failed to post comment"
                }
            } catch (_: Exception) {
                _message.value = "Network error"
            }
        }
    }

    fun deleteComment(postId: Int, commentId: Int) {
        _comments.value = _comments.value?.filter { it.id != commentId }
        _commentCount.value = maxOf(0, (_commentCount.value ?: 1) - 1)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication()).deleteComment(commentId)
                if (!response.isSuccessful && response.code() != 204) {
                    loadPost(postId)
                    _message.value = "Failed to delete comment"
                }
            } catch (_: Exception) {
                loadPost(postId)
            }
        }
    }

    fun reportPost(postId: Int, reason: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(getApplication())
                    .reportPost(postId, ReportRequest(reason))
                if (response.isSuccessful) {
                    _message.value = "Report submitted. Thank you."
                } else {
                    val errorBody = response.errorBody()?.string()
                    _message.value = if (errorBody?.contains("already reported") == true)
                        "You have already reported this post."
                    else "Could not submit report."
                }
            } catch (_: Exception) {
                _message.value = "Network error"
            }
        }
    }

    fun clearMessage() { _message.value = null }
}
