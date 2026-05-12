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
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.VoteRequest
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.util.BadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MyPostsActivity : AppCompatActivity() {

    private enum class Tab {
        FORUM_POSTS,
        HELP_REQUESTS,
        HELP_OFFERS
    }

    private lateinit var tokenManager: TokenManager
    private lateinit var postAdapter: PostAdapter
    private lateinit var requestAdapter: HelpRequestAdapter
    private lateinit var offerAdapter: HelpOfferAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var txtState: TextView
    private lateinit var tabForumPosts: TextView
    private lateinit var tabHelpRequests: TextView
    private lateinit var tabHelpOffers: TextView

    private var activeTab = Tab.FORUM_POSTS
    private var posts: List<Post> = emptyList()
    private var requests: List<HelpRequestItem> = emptyList()
    private var offers: List<HelpOfferItem> = emptyList()

    private val postDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedPostId = result.data?.getIntExtra("deleted_post_id", -1) ?: -1
            if (deletedPostId != -1) {
                posts = posts.filter { it.id != deletedPostId }
                displayForumPosts()
            } else {
                loadActiveTab()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        tokenManager = TokenManager(this)
        swipeRefresh = findViewById(R.id.swipeRefreshMyPosts)
        txtState = findViewById(R.id.txtMyPostsState)
        recyclerView = findViewById(R.id.recyclerMyPosts)
        tabForumPosts = findViewById(R.id.tabForumPosts)
        tabHelpRequests = findViewById(R.id.tabHelpRequests)
        tabHelpOffers = findViewById(R.id.tabHelpOffers)

        findViewById<TextView>(R.id.linkProfile).setOnClickListener { finish() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        setupAdapters()
        setupTabs()

        swipeRefresh.setColorSchemeResources(R.color.accent)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface)
        swipeRefresh.setOnRefreshListener { loadActiveTab() }

        selectTab(Tab.FORUM_POSTS)
    }

    override fun onResume() {
        super.onResume()
        if (::recyclerView.isInitialized) loadActiveTab()
    }

    private fun setupAdapters() {
        val currentUserId = tokenManager.getUser()?.id

        postAdapter = PostAdapter(
            currentUserId = currentUserId,
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

        requestAdapter = HelpRequestAdapter(emptyList(), currentUserId) { item ->
            val intent = Intent(this, HelpRequestDetailActivity::class.java)
            intent.putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, item.id)
            startActivity(intent)
        }

        offerAdapter = HelpOfferAdapter(
            items = mutableListOf(),
            currentUserId = currentUserId,
            onItemClick = { showOfferDetailDialog(it) },
            onDeleteClick = { confirmDeleteOffer(it) }
        )
    }

    private fun setupTabs() {
        tabForumPosts.setOnClickListener { selectTab(Tab.FORUM_POSTS) }
        tabHelpRequests.setOnClickListener { selectTab(Tab.HELP_REQUESTS) }
        tabHelpOffers.setOnClickListener { selectTab(Tab.HELP_OFFERS) }
    }

    private fun selectTab(tab: Tab) {
        activeTab = tab
        updateTabStyles()
        recyclerView.adapter = when (tab) {
            Tab.FORUM_POSTS -> postAdapter
            Tab.HELP_REQUESTS -> requestAdapter
            Tab.HELP_OFFERS -> offerAdapter
        }
        loadActiveTab()
    }

    private fun updateTabStyles() {
        val tabs = mapOf(
            Tab.FORUM_POSTS to tabForumPosts,
            Tab.HELP_REQUESTS to tabHelpRequests,
            Tab.HELP_OFFERS to tabHelpOffers
        )

        for ((tab, view) in tabs) {
            if (tab == activeTab) {
                view.setTextColor(getColor(R.color.accent))
                view.setBackgroundResource(R.drawable.sort_pill_active_bg)
            } else {
                view.setTextColor(getColor(R.color.text_muted))
                view.setBackgroundResource(R.drawable.sort_pill_bg)
            }
        }
    }

    private fun loadActiveTab() {
        val userId = tokenManager.getUser()?.id
        if (userId == null) {
            txtState.text = getString(R.string.my_posts_sign_in_required)
            txtState.visibility = View.VISIBLE
            swipeRefresh.isRefreshing = false
            clearActiveAdapter()
            return
        }

        txtState.text = getString(R.string.my_posts_loading)
        txtState.visibility = View.VISIBLE

        when (activeTab) {
            Tab.FORUM_POSTS -> loadForumPosts(userId)
            Tab.HELP_REQUESTS -> loadHelpRequests(userId)
            Tab.HELP_OFFERS -> loadHelpOffers(userId)
        }
    }

    private fun loadForumPosts(userId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@MyPostsActivity)
                    .getPosts(author = userId)

                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    posts = response.body() ?: emptyList()
                    displayForumPosts()
                } else {
                    showError()
                }
            } catch (_: Exception) {
                swipeRefresh.isRefreshing = false
                showError()
            }
        }
    }

    private fun loadHelpRequests(userId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@MyPostsActivity)
                    .getHelpRequests(author = userId)

                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    requests = response.body() ?: emptyList()
                    displayHelpRequests()
                } else {
                    showError()
                }
            } catch (_: Exception) {
                swipeRefresh.isRefreshing = false
                showError()
            }
        }
    }

    private fun loadHelpOffers(userId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@MyPostsActivity)
                    .getHelpOffers(author = userId)

                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    offers = response.body() ?: emptyList()
                    displayHelpOffers()
                } else {
                    showError()
                }
            } catch (_: Exception) {
                swipeRefresh.isRefreshing = false
                showError()
            }
        }
    }

    private fun displayForumPosts() {
        val sorted = posts.sortedByDescending { it.createdAt }
        postAdapter.submitList(sorted)
        showEmptyIfNeeded(sorted.isEmpty())
    }

    private fun displayHelpRequests() {
        val sorted = requests.sortedByDescending { it.createdAt }
        requestAdapter.updateItems(sorted)
        showEmptyIfNeeded(sorted.isEmpty())
    }

    private fun displayHelpOffers() {
        val sorted = offers.sortedByDescending { it.createdAt }
        offerAdapter.updateItems(sorted)
        showEmptyIfNeeded(sorted.isEmpty())
    }

    private fun showEmptyIfNeeded(isEmpty: Boolean) {
        txtState.text = when (activeTab) {
            Tab.FORUM_POSTS -> getString(R.string.my_posts_empty_forum_posts)
            Tab.HELP_REQUESTS -> getString(R.string.my_posts_empty_help_requests)
            Tab.HELP_OFFERS -> getString(R.string.my_posts_empty_help_offers)
        }
        txtState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showError() {
        txtState.text = when (activeTab) {
            Tab.FORUM_POSTS -> getString(R.string.my_posts_load_failed_forum_posts)
            Tab.HELP_REQUESTS -> getString(R.string.my_posts_load_failed_help_requests)
            Tab.HELP_OFFERS -> getString(R.string.my_posts_load_failed_help_offers)
        }
        txtState.visibility = View.VISIBLE
        Toast.makeText(this, txtState.text, Toast.LENGTH_SHORT).show()
    }

    private fun clearActiveAdapter() {
        when (activeTab) {
            Tab.FORUM_POSTS -> postAdapter.submitList(emptyList())
            Tab.HELP_REQUESTS -> requestAdapter.updateItems(emptyList())
            Tab.HELP_OFFERS -> offerAdapter.updateItems(emptyList())
        }
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
                    displayForumPosts()
                    Toast.makeText(this@MyPostsActivity, getString(R.string.forum_post_deleted), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MyPostsActivity, getString(R.string.forum_delete_failed), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@MyPostsActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOfferDetailDialog(item: HelpOfferItem) {
        val message = buildString {
            append(getString(R.string.help_offer_detail_category_format, BadgeUtils.formatCategoryLabel(this@MyPostsActivity, item.category)))
            append('\n')
            append(getString(R.string.help_offer_detail_availability_format, BadgeUtils.formatAvailabilityLabel(this@MyPostsActivity, item.availability)))
            append("\n\n")
            append(item.description)
            append("\n\n")
            append(getString(R.string.help_offer_author_format, item.author.fullName))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(item.skillOrResource)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private fun confirmDeleteOffer(item: HelpOfferItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_offer))
            .setMessage(getString(R.string.delete_offer_confirm))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteOffer(item) }
            .show()
    }

    private fun deleteOffer(item: HelpOfferItem) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@MyPostsActivity)
                    .deleteHelpOffer(item.id)

                if (response.code() == 204 || response.isSuccessful) {
                    offers = offers.filter { it.id != item.id }
                    offerAdapter.removeItem(item.id)
                    Toast.makeText(this@MyPostsActivity, getString(R.string.help_offer_deleted), Toast.LENGTH_SHORT).show()
                    showEmptyIfNeeded(offers.isEmpty())
                } else {
                    Toast.makeText(this@MyPostsActivity, getString(R.string.help_offer_delete_failed), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@MyPostsActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
