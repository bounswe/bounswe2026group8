package com.bounswe2026group8.emergencyhub.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.CreateCommentRequest
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.RepostRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.VoteRequest
import com.bounswe2026group8.emergencyhub.auth.HubManager
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PostDetailActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var commentAdapter: CommentAdapter

    private var postId = -1
    private var post: Post? = null
    private var commentCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        tokenManager = TokenManager(this)
        postId = intent.getIntExtra("post_id", -1)
        if (postId == -1) { finish(); return }

        val currentUserId = tokenManager.getUser()?.id
        commentAdapter = CommentAdapter(
            currentUserId = currentUserId,
            onDeleteClick = { comment -> deleteComment(comment.id) }
        )

        val recyclerComments = findViewById<RecyclerView>(R.id.recyclerComments)
        recyclerComments.layoutManager = LinearLayoutManager(this)
        recyclerComments.adapter = commentAdapter

        if (tokenManager.isLoggedIn()) {
            findViewById<MaterialCardView>(R.id.commentInputCard).visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btnPostComment).setOnClickListener { submitComment() }
        }

        findViewById<TextView>(R.id.linkBackToForum).setOnClickListener { finish() }

        HubSelectorHelper(this, findViewById<Spinner>(R.id.spinnerHubSelector)).load()

        setupVoteButtons()
        loadPostAndComments()
    }

    private fun loadPostAndComments() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getService(this@PostDetailActivity)
                val postResponse = api.getPost(postId)
                val commentsResponse = api.getComments(postId)

                if (postResponse.isSuccessful) {
                    post = postResponse.body()
                    post?.let { displayPost(it) }
                } else {
                    Toast.makeText(this@PostDetailActivity, "Post not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                if (commentsResponse.isSuccessful) {
                    val comments = commentsResponse.body() ?: emptyList()
                    commentAdapter.submitList(comments)
                    updateCommentsUI()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayPost(p: Post) {
        commentCount = p.commentCount

        val txtRepostLabel = findViewById<TextView>(R.id.txtRepostLabel)
        if (p.repostedFrom != null) {
            txtRepostLabel.text = "🔁 ${p.author.fullName} reposted"
            txtRepostLabel.visibility = View.VISIBLE
        } else {
            txtRepostLabel.visibility = View.GONE
        }

        val txtForumType = findViewById<TextView>(R.id.txtForumTypeBadge)
        when (p.forumType) {
            "GLOBAL" -> {
                txtForumType.text = "Global"
                txtForumType.setTextColor(getColor(R.color.forum_global))
                txtForumType.setBackgroundResource(R.drawable.forum_type_badge_global)
            }
            "STANDARD" -> {
                txtForumType.text = "Standard"
                txtForumType.setTextColor(getColor(R.color.forum_standard))
                txtForumType.setBackgroundResource(R.drawable.forum_type_badge_standard)
            }
            "URGENT" -> {
                txtForumType.text = "Urgent"
                txtForumType.setTextColor(getColor(R.color.forum_urgent))
                txtForumType.setBackgroundResource(R.drawable.forum_type_badge_urgent)
            }
        }

        findViewById<TextView>(R.id.txtHubBadge).text = p.hubName ?: "Global"
        findViewById<TextView>(R.id.txtPostTitle).text = p.title
        val displayAuthor = if (p.repostedFrom != null) p.repostedFrom.author.fullName else p.author.fullName
        findViewById<TextView>(R.id.txtPostAuthor).text = displayAuthor
        findViewById<TextView>(R.id.txtPostTime).text = timeAgo(p.createdAt)
        findViewById<TextView>(R.id.txtPostContent).text = p.content ?: ""

        // Image gallery
        val imageGallery = findViewById<LinearLayout>(R.id.imageGallery)
        val images = p.imageUrls
        if (!images.isNullOrEmpty()) {
            imageGallery.removeAllViews()
            imageGallery.visibility = View.VISIBLE
            val imageList = ArrayList(images)
            for ((index, url) in images.withIndex()) {
                val imgView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.post_detail_image_height)
                    ).apply {
                        if (index > 0) topMargin = 8
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    contentDescription = "Attachment ${index + 1}"
                    setOnClickListener {
                        val intent = Intent(this@PostDetailActivity, ImageLightboxActivity::class.java)
                        intent.putStringArrayListExtra("image_urls", imageList)
                        intent.putExtra("start_index", index)
                        startActivity(intent)
                    }
                }
                Glide.with(this).load(url).centerCrop().into(imgView)
                imageGallery.addView(imgView)
            }
        } else {
            imageGallery.visibility = View.GONE
        }

        val txtRepost = findViewById<TextView>(R.id.txtRepostCount)
        if (p.repostedFrom == null && p.repostCount > 0) {
            val word = if (p.repostCount == 1) "repost" else "reposts"
            txtRepost.text = "${p.repostCount} $word"
            txtRepost.visibility = View.VISIBLE
        } else {
            txtRepost.visibility = View.GONE
        }

        updateVoteDisplay()
        updateShareRepostButtons(p)
        updateCommentsUI()
    }

    // ── Share & Repost ──────────────────────────────────────────────────────

    private fun updateShareRepostButtons(p: Post) {
        val btnShare = findViewById<TextView>(R.id.btnShare)
        val btnRepost = findViewById<TextView>(R.id.btnRepost)
        val txtRepostedLabel = findViewById<TextView>(R.id.txtRepostedLabel)

        // Share: always visible, copies link to clipboard
        btnShare.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Post Link", "https://emergencyhub.app/forum/posts/${p.id}")
            clipboard.setPrimaryClip(clip)
            btnShare.text = "Copied!"
            btnShare.postDelayed({ btnShare.text = "Share" }, 1500)
        }

        // Repost rules:
        // 1. Must be logged in
        // 2. Cannot repost own post
        // 3. Cannot repost if already reposted
        val currentUserId = tokenManager.getUser()?.id
        val isAuthor = currentUserId != null && currentUserId == p.author.id
        val alreadyReposted = p.userHasReposted == true

        if (tokenManager.isLoggedIn() && !isAuthor && !alreadyReposted) {
            btnRepost.visibility = View.VISIBLE
            txtRepostedLabel.visibility = View.GONE
            btnRepost.setOnClickListener { handleRepost() }
        } else if (tokenManager.isLoggedIn() && !isAuthor && alreadyReposted) {
            btnRepost.visibility = View.GONE
            txtRepostedLabel.visibility = View.VISIBLE
        } else {
            btnRepost.visibility = View.GONE
            txtRepostedLabel.visibility = View.GONE
        }
    }

    private fun handleRepost() {
        lifecycleScope.launch {
            try {
                val hubId = HubManager(this@PostDetailActivity).getSelectedHub()?.id
                val response = RetrofitClient.getService(this@PostDetailActivity)
                    .repost(postId, RepostRequest(hub = hubId))

                if (response.isSuccessful) {
                    post = post?.copy(
                        userHasReposted = true,
                        repostCount = (post?.repostCount ?: 0) + 1
                    )
                    post?.let {
                        // Update repost count display
                        val txtRepost = findViewById<TextView>(R.id.txtRepostCount)
                        if (it.repostedFrom == null && it.repostCount > 0) {
                            val word = if (it.repostCount == 1) "repost" else "reposts"
                            txtRepost.text = "${it.repostCount} $word"
                            txtRepost.visibility = View.VISIBLE
                        }
                        updateShareRepostButtons(it)
                    }
                    Toast.makeText(this@PostDetailActivity, "Reposted!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val msg = try {
                        com.google.gson.JsonParser().parse(errorBody).asJsonObject
                            .get("detail")?.asString ?: "Could not repost."
                    } catch (_: Exception) { "Could not repost." }
                    Toast.makeText(this@PostDetailActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Voting ──────────────────────────────────────────────────────────────

    private fun setupVoteButtons() {
        findViewById<TextView>(R.id.btnUpvote).setOnClickListener { handleVote("UP") }
        findViewById<TextView>(R.id.btnDownvote).setOnClickListener { handleVote("DOWN") }
    }

    private fun handleVote(type: String) {
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Sign in to vote", Toast.LENGTH_SHORT).show()
            return
        }

        val p = post ?: return
        val prevPost = p

        var newUp = p.upvoteCount
        var newDown = p.downvoteCount
        var newUserVote: String? = type

        if (p.userVote == type) {
            if (type == "UP") newUp-- else newDown--
            newUserVote = null
        } else if (p.userVote != null) {
            if (p.userVote == "UP") { newUp--; newDown++ }
            else { newDown--; newUp++ }
        } else {
            if (type == "UP") newUp++ else newDown++
        }

        post = p.copy(upvoteCount = newUp, downvoteCount = newDown, userVote = newUserVote)
        updateVoteDisplay()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@PostDetailActivity)
                    .vote(postId, VoteRequest(type))
                if (!response.isSuccessful) {
                    post = prevPost
                    updateVoteDisplay()
                }
            } catch (_: Exception) {
                post = prevPost
                updateVoteDisplay()
            }
        }
    }

    private fun updateVoteDisplay() {
        val p = post ?: return
        val btnUp = findViewById<TextView>(R.id.btnUpvote)
        val btnDown = findViewById<TextView>(R.id.btnDownvote)

        btnUp.text = "▲ ${p.upvoteCount}"
        btnDown.text = "▼ ${p.downvoteCount}"

        btnUp.setTextColor(
            if (p.userVote == "UP") getColor(R.color.vote_up_active)
            else getColor(R.color.text_muted)
        )
        btnUp.setBackgroundResource(
            if (p.userVote == "UP") R.drawable.sort_pill_active_bg
            else R.drawable.sort_pill_bg
        )

        btnDown.setTextColor(
            if (p.userVote == "DOWN") getColor(R.color.vote_down_active)
            else getColor(R.color.text_muted)
        )
        btnDown.setBackgroundResource(
            if (p.userVote == "DOWN") R.drawable.forum_type_badge_urgent
            else R.drawable.sort_pill_bg
        )
    }

    // ── Comments ────────────────────────────────────────────────────────────

    private fun updateCommentsUI() {
        val heading = findViewById<TextView>(R.id.txtCommentsHeading)
        heading.text = "Comments ($commentCount)"

        val txtNoComments = findViewById<TextView>(R.id.txtNoComments)
        txtNoComments.visibility = if (commentAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun submitComment() {
        val input = findViewById<TextInputEditText>(R.id.inputComment)
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        val btn = findViewById<MaterialButton>(R.id.btnPostComment)
        btn.isEnabled = false
        btn.text = "Posting…"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@PostDetailActivity)
                    .createComment(postId, CreateCommentRequest(text))

                if (response.isSuccessful) {
                    val comment = response.body()!!
                    commentAdapter.addComment(comment)
                    commentCount++
                    input.text?.clear()
                    updateCommentsUI()
                } else {
                    Toast.makeText(this@PostDetailActivity, "Failed to post comment", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                btn.isEnabled = true
                btn.text = "Post Comment"
            }
        }
    }

    private fun deleteComment(commentId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@PostDetailActivity)
                    .deleteComment(commentId)

                if (response.isSuccessful || response.code() == 204) {
                    commentAdapter.removeComment(commentId)
                    commentCount = maxOf(0, commentCount - 1)
                    updateCommentsUI()
                } else {
                    Toast.makeText(this@PostDetailActivity, "Failed to delete comment", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@PostDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun timeAgo(dateStr: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            val date = format.parse(dateStr.substringBefore(".").substringBefore("Z"))
                ?: return dateStr
            val seconds = (Date().time - date.time) / 1000

            when {
                seconds < 60 -> "just now"
                seconds < 3600 -> "${seconds / 60}m ago"
                seconds < 86400 -> "${seconds / 3600}h ago"
                else -> "${seconds / 86400}d ago"
            }
        } catch (_: Exception) {
            dateStr
        }
    }
}
