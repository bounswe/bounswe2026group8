package com.bounswe2026group8.emergencyhub.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.VoteRequest
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import kotlinx.coroutines.launch

class MyPostsActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var postAdapter: PostAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var txtState: TextView

    private var posts: List<Post> = emptyList()

    private val postDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedPostId = result.data?.getIntExtra("deleted_post_id", -1) ?: -1
            if (deletedPostId != -1) {
                posts = posts.filter { it.id != deletedPostId }
                displayPosts()
            } else {
                loadPosts()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        tokenManager = TokenManager(this)
        swipeRefresh = findViewById(R.id.swipeRefreshMyPosts)
        txtState = findViewById(R.id.txtMyPostsState)

        findViewById<TextView>(R.id.linkProfile).setOnClickListener { finish() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        val recycler = findViewById<RecyclerView>(R.id.recyclerMyPosts)
        postAdapter = PostAdapter(
            currentUserId = tokenManager.getUser()?.id,
            isLoggedIn = tokenManager.isLoggedIn(),
            onVoteClick = { postId, voteType -> handleVote(postId, voteType) },
            onRepostClick = null,
            onDeleteClick = { postId -> confirmAndDeletePost(postId) },
            onPostClick = { postId ->
                val intent = Intent(this, PostDetailActivity::class.java)
                intent.putExtra("post_id", postId)
                postDetailLauncher.launch(intent)
            }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = postAdapter

        swipeRefresh.setColorSchemeResources(R.color.accent)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface)
        swipeRefresh.setOnRefreshListener { loadPosts() }

        loadPosts()
    }

    override fun onResume() {
        super.onResume()
        loadPosts()
    }

    private fun loadPosts() {
        val userId = tokenManager.getUser()?.id
        if (userId == null) {
            txtState.text = getString(R.string.my_posts_sign_in_required)
            txtState.visibility = View.VISIBLE
            swipeRefresh.isRefreshing = false
            postAdapter.submitList(emptyList())
            return
        }

        txtState.text = getString(R.string.my_posts_loading)
        txtState.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@MyPostsActivity)
                    .getPosts(author = userId)

                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    posts = response.body() ?: emptyList()
                    displayPosts()
                } else {
                    showError()
                }
            } catch (_: Exception) {
                swipeRefresh.isRefreshing = false
                showError()
            }
        }
    }

    private fun displayPosts() {
        val sorted = posts.sortedByDescending { it.createdAt }
        postAdapter.submitList(sorted)
        txtState.text = getString(R.string.my_posts_empty)
        txtState.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showError() {
        txtState.text = getString(R.string.my_posts_load_failed)
        txtState.visibility = View.VISIBLE
        Toast.makeText(this, getString(R.string.my_posts_load_failed), Toast.LENGTH_SHORT).show()
    }

    private fun handleVote(postId: Int, voteType: String) {
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.sign_in_to_vote), Toast.LENGTH_SHORT).show()
            return
        }

        val idx = posts.indexOfFirst { it.id == postId }
        if (idx == -1) return
        val post = posts[idx]

        var newUp = post.upvoteCount
        var newDown = post.downvoteCount
        var newUserVote: String? = voteType

        if (post.userVote == voteType) {
            if (voteType == "UP") newUp-- else newDown--
            newUserVote = null
        } else if (post.userVote != null) {
            if (post.userVote == "UP") {
                newUp--
                newDown++
            } else {
                newDown--
                newUp++
            }
        } else {
            if (voteType == "UP") newUp++ else newDown++
        }

        val updated = post.copy(upvoteCount = newUp, downvoteCount = newDown, userVote = newUserVote)
        posts = posts.toMutableList().also { it[idx] = updated }
        postAdapter.updatePost(postId) { updated }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@MyPostsActivity)
                    .vote(postId, VoteRequest(voteType))
                if (!response.isSuccessful) {
                    posts = posts.toMutableList().also { it[idx] = post }
                    postAdapter.updatePost(postId) { post }
                }
            } catch (_: Exception) {
                posts = posts.toMutableList().also { it[idx] = post }
                postAdapter.updatePost(postId) { post }
            }
        }
    }

    private fun confirmAndDeletePost(postId: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.forum_delete_post_title))
            .setMessage(getString(R.string.forum_delete_post_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deletePost(postId) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deletePost(postId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@MyPostsActivity).deletePost(postId)
                if (response.isSuccessful || response.code() == 204) {
                    posts = posts.filter { it.id != postId }
                    displayPosts()
                    Toast.makeText(this@MyPostsActivity, getString(R.string.forum_post_deleted), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MyPostsActivity, getString(R.string.forum_delete_failed), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@MyPostsActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
