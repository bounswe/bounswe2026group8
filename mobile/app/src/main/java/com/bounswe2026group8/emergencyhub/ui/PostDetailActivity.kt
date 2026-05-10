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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.CreateCommentRequest
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.ReportRequest
import com.bounswe2026group8.emergencyhub.api.RepostRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.VoteRequest
import com.bounswe2026group8.emergencyhub.auth.HubManager
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.util.TimeUtils
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import com.bounswe2026group8.emergencyhub.util.VoiceInputManager

class PostDetailActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var commentAdapter: CommentAdapter

    private var postId = -1
    private var post: Post? = null
    private var commentCount = 0
    private lateinit var voiceInputManager: VoiceInputManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        tokenManager = TokenManager(this)
        voiceInputManager = VoiceInputManager(this)
        postId = intent.getIntExtra("post_id", -1)
        if (postId == -1) {
            finish()
            return
        }

        commentAdapter = CommentAdapter(
            currentUserId = tokenManager.getUser()?.id,
            onDeleteClick = { comment -> deleteComment(comment.id) }
        )

        findViewById<RecyclerView>(R.id.recyclerComments).apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = commentAdapter
        }

        if (tokenManager.isLoggedIn()) {
            findViewById<MaterialCardView>(R.id.commentInputCard).visibility = View.VISIBLE
            voiceInputManager.bind(findViewById<TextInputEditText>(R.id.inputComment))
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
                    Toast.makeText(this@PostDetailActivity, getString(R.string.forum_post_not_found), Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                if (commentsResponse.isSuccessful) {
                    commentAdapter.submitList(commentsResponse.body() ?: emptyList())
                    updateCommentsUI()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, getString(R.string.network_error_with_message, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayPost(p: Post) {
        commentCount = p.commentCount

        findViewById<TextView>(R.id.txtRepostLabel).apply {
            if (p.repostedFrom != null) {
                text = getString(R.string.forum_reposted_by_format, p.author.fullName)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        findViewById<TextView>(R.id.txtForumTypeBadge).apply {
            when (p.forumType) {
                "GLOBAL" -> {
                    text = getString(R.string.forum_type_global)
                    setTextColor(getColor(R.color.forum_global))
                    setBackgroundResource(R.drawable.forum_type_badge_global)
                }
                "STANDARD" -> {
                    text = getString(R.string.forum_type_standard)
                    setTextColor(getColor(R.color.forum_standard))
                    setBackgroundResource(R.drawable.forum_type_badge_standard)
                }
                else -> {
                    text = getString(R.string.forum_type_urgent)
                    setTextColor(getColor(R.color.forum_urgent))
                    setBackgroundResource(R.drawable.forum_type_badge_urgent)
                }
            }
        }

        findViewById<TextView>(R.id.txtHubBadge).text = p.hubName ?: getString(R.string.forum_type_global)
        findViewById<TextView>(R.id.txtPostTitle).text = p.title
        findViewById<TextView>(R.id.txtPostAuthor).text =
            if (p.repostedFrom != null) p.repostedFrom.author.fullName else p.author.fullName
        findViewById<TextView>(R.id.txtPostTime).text = TimeUtils.timeAgo(p.createdAt)
        findViewById<TextView>(R.id.txtPostContent).text = p.content ?: ""

        val imageGallery = findViewById<LinearLayout>(R.id.imageGallery)
        if (!p.imageUrls.isNullOrEmpty()) {
            imageGallery.removeAllViews()
            imageGallery.visibility = View.VISIBLE
            val imageList = ArrayList(p.imageUrls)
            for ((index, url) in p.imageUrls.withIndex()) {
                val imgView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.post_detail_image_height)
                    ).apply { if (index > 0) topMargin = 8 }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    contentDescription = getString(R.string.attachment_content_description, index + 1)
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

        findViewById<TextView>(R.id.txtRepostCount).apply {
            if (p.repostedFrom == null && p.repostCount > 0) {
                val word = if (p.repostCount == 1) getString(R.string.repost_singular) else getString(R.string.repost_plural)
                text = getString(R.string.forum_repost_count_format, p.repostCount, word)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        updateVoteDisplay()
        updateShareRepostButtons(p)
        updateDeleteButton(p)
        updateReportButton(p)
        updateCommentsUI()
    }

    private fun updateDeleteButton(p: Post) {
        val btnDelete = findViewById<TextView>(R.id.btnDeletePost)
        val isAuthor = tokenManager.getUser()?.id == p.author.id
        if (tokenManager.isLoggedIn() && isAuthor) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.forum_delete_post_title))
                    .setMessage(getString(R.string.forum_delete_post_confirm))
                    .setPositiveButton(getString(R.string.delete)) { _, _ -> deletePost() }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        } else {
            btnDelete.visibility = View.GONE
        }
    }

    private fun deletePost() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@PostDetailActivity).deletePost(postId)
                if (response.isSuccessful || response.code() == 204) {
                    Toast.makeText(this@PostDetailActivity, getString(R.string.forum_post_deleted), Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK, Intent().putExtra("deleted_post_id", postId))
                    finish()
                } else {
                    Toast.makeText(this@PostDetailActivity, getString(R.string.forum_delete_failed), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@PostDetailActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateReportButton(p: Post) {
        val btnReport = findViewById<TextView>(R.id.btnReportPost)
        val isAuthor = tokenManager.getUser()?.id == p.author.id
        if (tokenManager.isLoggedIn() && !isAuthor) {
            btnReport.visibility = View.VISIBLE
            btnReport.setOnClickListener { showReportDialog() }
        } else {
            btnReport.visibility = View.GONE
        }
    }

    private fun showReportDialog() {
        val reasons = arrayOf(
            getString(R.string.report_reason_spam),
            getString(R.string.report_reason_misinformation),
            getString(R.string.report_reason_abuse),
            getString(R.string.report_reason_irrelevant)
        )
        val reasonValues = arrayOf("SPAM", "MISINFORMATION", "ABUSE", "IRRELEVANT")
        var selectedIndex = 0
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.forum_report_title))
            .setSingleChoiceItems(reasons, 0) { _, which -> selectedIndex = which }
            .setPositiveButton(getString(R.string.submit)) { _, _ -> reportPost(reasonValues[selectedIndex]) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun reportPost(reason: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@PostDetailActivity)
                    .reportPost(postId, ReportRequest(reason))
                if (response.isSuccessful) {
                    Toast.makeText(this@PostDetailActivity, getString(R.string.forum_report_success), Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = if (errorBody?.contains("already reported") == true) {
                        getString(R.string.forum_report_already_exists)
                    } else {
                        getString(R.string.forum_report_failed)
                    }
                    Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@PostDetailActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateShareRepostButtons(p: Post) {
        val btnShare = findViewById<TextView>(R.id.btnShare)
        val btnRepost = findViewById<TextView>(R.id.btnRepost)
        val txtRepostedLabel = findViewById<TextView>(R.id.txtRepostedLabel)

        btnShare.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.forum_post_link_label), "https://emergencyhub.app/forum/posts/${p.id}")
            clipboard.setPrimaryClip(clip)
            btnShare.text = getString(R.string.copied)
            btnShare.postDelayed({ btnShare.text = getString(R.string.share) }, 1500)
        }

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
                    post = post?.copy(userHasReposted = true, repostCount = (post?.repostCount ?: 0) + 1)
                    post?.let { displayPost(it) }
                    Toast.makeText(this@PostDetailActivity, getString(R.string.forum_reposted), Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val msg = try {
                        com.google.gson.JsonParser().parse(errorBody).asJsonObject
                            .get("detail")?.asString ?: getString(R.string.forum_repost_failed)
                    } catch (_: Exception) {
                        getString(R.string.forum_repost_failed)
                    }
                    Toast.makeText(this@PostDetailActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@PostDetailActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupVoteButtons() {
        findViewById<TextView>(R.id.btnUpvote).setOnClickListener { handleVote("UP") }
        findViewById<TextView>(R.id.btnDownvote).setOnClickListener { handleVote("DOWN") }
    }

    private fun handleVote(type: String) {
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.sign_in_to_vote), Toast.LENGTH_SHORT).show()
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
            if (p.userVote == "UP") {
                newUp--
                newDown++
            } else {
                newDown--
                newUp++
            }
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
        findViewById<TextView>(R.id.btnUpvote).apply {
            text = "\u25B2 ${p.upvoteCount}"
            setTextColor(if (p.userVote == "UP") getColor(R.color.vote_up_active) else getColor(R.color.text_muted))
            setBackgroundResource(if (p.userVote == "UP") R.drawable.sort_pill_active_bg else R.drawable.sort_pill_bg)
        }
        findViewById<TextView>(R.id.btnDownvote).apply {
            text = "\u25BC ${p.downvoteCount}"
            setTextColor(if (p.userVote == "DOWN") getColor(R.color.vote_down_active) else getColor(R.color.text_muted))
            setBackgroundResource(if (p.userVote == "DOWN") R.drawable.forum_type_badge_urgent else R.drawable.sort_pill_bg)
        }
    }

    private fun updateCommentsUI() {
        findViewById<TextView>(R.id.txtCommentsHeading).text =
            getString(R.string.comments_count_label, commentCount)
        findViewById<TextView>(R.id.txtNoComments).visibility =
            if (commentAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun submitComment() {
        val input = findViewById<TextInputEditText>(R.id.inputComment)
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        val btn = findViewById<MaterialButton>(R.id.btnPostComment)
        btn.isEnabled = false
        btn.text = getString(R.string.posting)

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
                    Toast.makeText(this@PostDetailActivity, getString(R.string.comment_post_failed), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@PostDetailActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
            } finally {
                btn.isEnabled = true
                btn.text = getString(R.string.post_comment)
            }
        }
    }

    private fun deleteComment(commentId: Int) {
        commentAdapter.removeComment(commentId)
        commentCount = maxOf(0, commentCount - 1)
        updateCommentsUI()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@PostDetailActivity).deleteComment(commentId)
                if (!response.isSuccessful && response.code() != 204) {
                    loadPostAndComments()
                    Toast.makeText(this@PostDetailActivity, getString(R.string.comment_delete_failed), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                loadPostAndComments()
            }
        }
    }
}
