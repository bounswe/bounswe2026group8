package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Hub
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.RepostRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.VoteRequest
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import kotlinx.coroutines.launch

class ForumActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var postAdapter: PostAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var txtEmptyState: TextView

    private lateinit var hubSelectorHelper: HubSelectorHelper

    private var currentTab = "GLOBAL"
    private var currentSort = "newest"
    private var posts: List<Post> = emptyList()
    private var selectedHub: Hub? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum)

        tokenManager = TokenManager(this)

        recyclerView = findViewById(R.id.recyclerPosts)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        txtEmptyState = findViewById(R.id.txtEmptyState)

        postAdapter = PostAdapter(
            currentUserId = tokenManager.getUser()?.id,
            isLoggedIn = tokenManager.isLoggedIn(),
            onVoteClick = { postId, voteType -> handleVote(postId, voteType) },
            onRepostClick = { postId -> handleRepost(postId) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = postAdapter

        swipeRefresh.setColorSchemeResources(R.color.accent)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface)
        swipeRefresh.setOnRefreshListener { loadPosts() }

        hubSelectorHelper = HubSelectorHelper(
            this,
            findViewById<Spinner>(R.id.spinnerHubSelector),
            onHubSelected = { hub ->
                selectedHub = hub
                updateHubLabel()
                if (currentTab != "GLOBAL") loadPosts()
            }
        )
        hubSelectorHelper.load()

        setupTabs()
        setupSortBar()
        setupNewPostButton()

        findViewById<TextView>(R.id.linkDashboard).setOnClickListener { finish() }

        loadPosts()
    }

    private data class TabInfo(
        val container: LinearLayout,
        val text: TextView,
        val line: View,
        val type: String,
        val activeColor: Int
    )

    private fun setupTabs() {
        val tabs = listOf(
            TabInfo(
                findViewById(R.id.tabGlobal),
                findViewById(R.id.tabGlobalText),
                findViewById(R.id.tabGlobalLine),
                "GLOBAL",
                getColor(R.color.forum_global)
            ),
            TabInfo(
                findViewById(R.id.tabStandard),
                findViewById(R.id.tabStandardText),
                findViewById(R.id.tabStandardLine),
                "STANDARD",
                getColor(R.color.forum_standard)
            ),
            TabInfo(
                findViewById(R.id.tabUrgent),
                findViewById(R.id.tabUrgentText),
                findViewById(R.id.tabUrgentLine),
                "URGENT",
                getColor(R.color.forum_urgent)
            )
        )

        fun selectTab(selected: TabInfo) {
            currentTab = selected.type
            for (tab in tabs) {
                if (tab == selected) {
                    tab.text.setTextColor(tab.activeColor)
                    tab.line.setBackgroundColor(tab.activeColor)
                } else {
                    tab.text.setTextColor(getColor(R.color.text_muted))
                    tab.line.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }

            updateHubLabel()
            loadPosts()
        }

        for (tab in tabs) {
            tab.container.setOnClickListener { selectTab(tab) }
        }

        selectTab(tabs[0])
    }

    private fun setupSortBar() {
        val sortNewest = findViewById<TextView>(R.id.sortNewest)
        val sortMostLiked = findViewById<TextView>(R.id.sortMostLiked)
        val sortHot = findViewById<TextView>(R.id.sortHot)

        val sortButtons = mapOf(
            sortNewest to "newest",
            sortMostLiked to "most_liked",
            sortHot to "hot"
        )

        fun selectSort(selected: TextView) {
            for ((view, sort) in sortButtons) {
                if (view == selected) {
                    view.setTextColor(getColor(R.color.accent))
                    view.setBackgroundResource(R.drawable.sort_pill_active_bg)
                    currentSort = sort
                } else {
                    view.setTextColor(getColor(R.color.text_muted))
                    view.setBackgroundResource(R.drawable.sort_pill_bg)
                }
            }
            applySortAndDisplay()
        }

        sortNewest.setOnClickListener { selectSort(sortNewest) }
        sortMostLiked.setOnClickListener { selectSort(sortMostLiked) }
        sortHot.setOnClickListener { selectSort(sortHot) }
    }

    private fun setupNewPostButton() {
        val btnNewPost = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNewPost)

        if (tokenManager.isLoggedIn()) {
            btnNewPost.visibility = View.VISIBLE
            btnNewPost.setOnClickListener {
                val intent = Intent(this, CreatePostActivity::class.java)
                intent.putExtra("forum_type", currentTab)
                startActivity(intent)
            }
        } else {
            btnNewPost.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        loadPosts()
    }

    private fun updateHubLabel() {
        val label = findViewById<TextView>(R.id.txtHubLabel)
        if (currentTab == "GLOBAL") {
            label.text = getString(R.string.forum_hub_all)
        } else {
            label.text = selectedHub?.name ?: getString(R.string.select_hub)
        }
    }

    private fun loadPosts() {
        txtEmptyState.visibility = View.GONE

        val hubId = if (currentTab == "GLOBAL") null else selectedHub?.id

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@ForumActivity)
                    .getPosts(forumType = currentTab, hub = hubId)

                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    posts = response.body() ?: emptyList()
                    applySortAndDisplay()

                    if (posts.isEmpty()) {
                        txtEmptyState.text = if (currentTab == "GLOBAL") {
                            getString(R.string.forum_empty_global)
                        } else {
                            getString(R.string.forum_empty_hub, currentTab.lowercase())
                        }
                        txtEmptyState.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        this@ForumActivity,
                        "Failed to load posts",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@ForumActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleVote(postId: Int, voteType: String) {
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Sign in to vote", Toast.LENGTH_SHORT).show()
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
            if (post.userVote == "UP") { newUp--; newDown++ }
            else { newDown--; newUp++ }
        } else {
            if (voteType == "UP") newUp++ else newDown++
        }

        val updated = post.copy(upvoteCount = newUp, downvoteCount = newDown, userVote = newUserVote)
        posts = posts.toMutableList().also { it[idx] = updated }
        postAdapter.updatePost(postId) { updated }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@ForumActivity)
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

    private fun handleRepost(postId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@ForumActivity)
                    .repost(postId, RepostRequest(hub = selectedHub?.id))

                if (response.isSuccessful) {
                    Toast.makeText(this@ForumActivity, "Reposted!", Toast.LENGTH_SHORT).show()
                    loadPosts()
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val msg = try {
                        com.google.gson.JsonParser().parse(errorBody).asJsonObject
                            .get("detail")?.asString ?: "Could not repost."
                    } catch (_: Exception) { "Could not repost." }
                    Toast.makeText(this@ForumActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ForumActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applySortAndDisplay() {
        val sorted = when (currentSort) {
            "most_liked" -> posts.sortedByDescending { it.upvoteCount }
            "hot" -> posts.sortedByDescending { hotScore(it) }
            else -> posts.sortedByDescending { it.createdAt }
        }
        postAdapter.submitList(sorted)

        txtEmptyState.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun hotScore(post: Post): Double {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = format.parse(post.createdAt.substringBefore(".").substringBefore("Z"))
            val ageMs = (System.currentTimeMillis() - (date?.time ?: 0)).toDouble()
            val twoDays = 2.0 * 24 * 60 * 60 * 1000
            if (ageMs > twoDays) return -1.0
            val activity = post.upvoteCount + post.downvoteCount + post.commentCount
            activity / (1.0 + ageMs / 3_600_000.0)
        } catch (_: Exception) {
            0.0
        }
    }
}
