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
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.viewmodel.PostDetailViewModel
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.bounswe2026group8.emergencyhub.util.TimeUtils

class PostDetailActivity : AppCompatActivity() {

    private val viewModel: PostDetailViewModel by viewModels()
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
            onDeleteClick = { comment -> viewModel.deleteComment(postId, comment.id) }
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
        observeViewModel()
        viewModel.loadPost(postId)
    }

    private fun observeViewModel() {
        viewModel.post.observe(this) { p ->
            post = p
            p?.let { displayPost(it) }
        }
        viewModel.comments.observe(this) { comments ->
            commentAdapter.submitList(comments)
            updateCommentsUI()
        }
        viewModel.commentCount.observe(this) { count ->
            commentCount = count
            updateCommentsUI()
        }
        viewModel.message.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
        viewModel.postDeleted.observe(this) { deleted ->
            if (deleted) {
                Toast.makeText(this, "Post deleted", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().putExtra("deleted_post_id", postId)
                setResult(RESULT_OK, resultIntent)
                finish()
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
        findViewById<TextView>(R.id.txtPostTime).text = TimeUtils.timeAgo(p.createdAt)
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
                Glide.with(this).load(RetrofitClient.resolveImageUrl(url)).centerCrop().into(imgView)
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
        updateDeleteButton(p)
        updateReportButton(p)
        updateCommentsUI()
    }

    private fun updateDeleteButton(p: Post) {
        val btnDelete = findViewById<TextView>(R.id.btnDeletePost)
        val currentUserId = tokenManager.getUser()?.id
        val isAuthor = currentUserId != null && currentUserId == p.author.id
        if (tokenManager.isLoggedIn() && isAuthor) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Post")
                    .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                    .setPositiveButton("Delete") { _, _ -> deletePost() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            btnDelete.visibility = View.GONE
        }
    }

    private fun deletePost() {
        viewModel.deletePost(postId)
    }

    // ── Report ──────────────────────────────────────────────────────────────

    private fun updateReportButton(p: Post) {
        val btnReport = findViewById<TextView>(R.id.btnReportPost)
        val currentUserId = tokenManager.getUser()?.id
        val isAuthor = currentUserId != null && currentUserId == p.author.id
        if (tokenManager.isLoggedIn() && !isAuthor) {
            btnReport.visibility = View.VISIBLE
            btnReport.setOnClickListener { showReportDialog() }
        } else {
            btnReport.visibility = View.GONE
        }
    }

    private fun showReportDialog() {
        val reasons = arrayOf("Spam", "Misinformation", "Abuse", "Irrelevant")
        val reasonValues = arrayOf("SPAM", "MISINFORMATION", "ABUSE", "IRRELEVANT")
        var selectedIndex = 0
        AlertDialog.Builder(this)
            .setTitle("Report this post")
            .setSingleChoiceItems(reasons, 0) { _, which -> selectedIndex = which }
            .setPositiveButton("Submit") { _, _ -> reportPost(reasonValues[selectedIndex]) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reportPost(reason: String) {
        viewModel.reportPost(postId, reason)
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
        viewModel.repost(postId)
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
        viewModel.vote(postId, type)
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

        viewModel.submitComment(postId, text)
        input.text?.clear()
    }
}
