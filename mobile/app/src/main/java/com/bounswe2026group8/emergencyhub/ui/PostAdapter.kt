package com.bounswe2026group8.emergencyhub.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PostAdapter(
    private var posts: MutableList<Post> = mutableListOf(),
    private val currentUserId: Int? = null,
    private val isLoggedIn: Boolean = false,
    private val onVoteClick: ((postId: Int, voteType: String) -> Unit)? = null,
    private val onRepostClick: ((postId: Int) -> Unit)? = null,
    private val onDeleteClick: ((postId: Int) -> Unit)? = null,
    private val onPostClick: ((postId: Int) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    fun submitList(newPosts: List<Post>) {
        posts = newPosts.toMutableList()
        notifyDataSetChanged()
    }

    fun updatePost(postId: Int, updater: (Post) -> Post) {
        val idx = posts.indexOfFirst { it.id == postId }
        if (idx != -1) {
            posts[idx] = updater(posts[idx])
            notifyItemChanged(idx)
        }
    }

    fun addPostToTop(post: Post) {
        posts.add(0, post)
        notifyItemInserted(0)
    }

    fun removePost(postId: Int) {
        val idx = posts.indexOfFirst { it.id == postId }
        if (idx != -1) {
            posts.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_card, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position], currentUserId, isLoggedIn, onVoteClick, onRepostClick, onDeleteClick, onPostClick)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtRepostLabel: TextView = itemView.findViewById(R.id.txtRepostLabel)
        private val txtUpvote: TextView = itemView.findViewById(R.id.txtUpvote)
        private val txtDownvote: TextView = itemView.findViewById(R.id.txtDownvote)
        private val txtForumType: TextView = itemView.findViewById(R.id.txtForumType)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        private val txtAuthor: TextView = itemView.findViewById(R.id.txtAuthor)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val txtComments: TextView = itemView.findViewById(R.id.txtComments)
        private val txtRepostCount: TextView = itemView.findViewById(R.id.txtRepostCount)
        private val btnShare: TextView = itemView.findViewById(R.id.btnShare)
        private val btnRepost: TextView = itemView.findViewById(R.id.btnRepost)
        private val txtReposted: TextView = itemView.findViewById(R.id.txtReposted)
        private val btnDeletePost: TextView = itemView.findViewById(R.id.btnDeletePost)
        private val imageContainer: LinearLayout = itemView.findViewById(R.id.imageContainer)
        private val imgThumb1: ImageView = itemView.findViewById(R.id.imgThumb1)
        private val imgThumb2: ImageView = itemView.findViewById(R.id.imgThumb2)
        private val imgThumb3: ImageView = itemView.findViewById(R.id.imgThumb3)
        private val txtMoreImages: TextView = itemView.findViewById(R.id.txtMoreImages)

        fun bind(
            post: Post,
            currentUserId: Int?,
            isLoggedIn: Boolean,
            onVoteClick: ((Int, String) -> Unit)?,
            onRepostClick: ((Int) -> Unit)?,
            onDeleteClick: ((Int) -> Unit)?,
            onPostClick: ((Int) -> Unit)?
        ) {
            // Repost label
            if (post.repostedFrom != null) {
                txtRepostLabel.text = "🔁 ${post.author.fullName} reposted"
                txtRepostLabel.visibility = View.VISIBLE
                txtAuthor.text = post.repostedFrom.author.fullName
            } else {
                txtRepostLabel.visibility = View.GONE
                txtAuthor.text = post.author.fullName
            }

            txtUpvote.text = "▲ ${post.upvoteCount}"
            txtDownvote.text = "▼ ${post.downvoteCount}"
            txtTitle.text = post.title
            txtTime.text = timeAgo(post.createdAt)

            // Image thumbnails
            val images = post.imageUrls
            if (!images.isNullOrEmpty()) {
                imageContainer.visibility = View.VISIBLE
                val thumbs = listOf(imgThumb1, imgThumb2, imgThumb3)
                for ((i, thumb) in thumbs.withIndex()) {
                    if (i < images.size) {
                        thumb.visibility = View.VISIBLE
                        Glide.with(itemView.context).load(RetrofitClient.resolveImageUrl(images[i])).centerCrop().into(thumb)
                    } else {
                        thumb.visibility = View.GONE
                    }
                }
                if (images.size > 3) {
                    txtMoreImages.text = "+${images.size - 3}"
                    txtMoreImages.visibility = View.VISIBLE
                } else {
                    txtMoreImages.visibility = View.GONE
                }
            } else {
                imageContainer.visibility = View.GONE
            }

            val commentWord = if (post.commentCount == 1) "comment" else "comments"
            txtComments.text = "${post.commentCount} $commentWord"

            // Repost count (only on non-repost posts)
            if (post.repostedFrom == null && post.repostCount > 0) {
                val repostWord = if (post.repostCount == 1) "repost" else "reposts"
                txtRepostCount.text = "${post.repostCount} $repostWord"
                txtRepostCount.visibility = View.VISIBLE
            } else {
                txtRepostCount.visibility = View.GONE
            }

            when (post.forumType) {
                "GLOBAL" -> {
                    txtForumType.text = "Global"
                    txtForumType.setTextColor(itemView.context.getColor(R.color.forum_global))
                    txtForumType.setBackgroundResource(R.drawable.forum_type_badge_global)
                    txtForumType.visibility = View.VISIBLE
                }
                "STANDARD" -> {
                    txtForumType.text = "Standard"
                    txtForumType.setTextColor(itemView.context.getColor(R.color.forum_standard))
                    txtForumType.setBackgroundResource(R.drawable.forum_type_badge_standard)
                    txtForumType.visibility = View.VISIBLE
                }
                "URGENT" -> {
                    txtForumType.text = "Urgent"
                    txtForumType.setTextColor(itemView.context.getColor(R.color.forum_urgent))
                    txtForumType.setBackgroundResource(R.drawable.forum_type_badge_urgent)
                    txtForumType.visibility = View.VISIBLE
                }
                else -> {
                    txtForumType.visibility = View.GONE
                }
            }

            txtUpvote.setTextColor(
                if (post.userVote == "UP") itemView.context.getColor(R.color.vote_up_active)
                else itemView.context.getColor(R.color.text_muted)
            )
            txtDownvote.setTextColor(
                if (post.userVote == "DOWN") itemView.context.getColor(R.color.vote_down_active)
                else itemView.context.getColor(R.color.text_muted)
            )

            txtUpvote.setOnClickListener { onVoteClick?.invoke(post.id, "UP") }
            txtDownvote.setOnClickListener { onVoteClick?.invoke(post.id, "DOWN") }

            // Share button
            btnShare.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Post Link", "https://emergencyhub.app/forum/posts/${post.id}")
                clipboard.setPrimaryClip(clip)
                btnShare.text = "Copied!"
                btnShare.postDelayed({ btnShare.text = "Share" }, 1500)
            }

            // Repost button rules:
            // Show if: logged in AND not the author AND not already reposted
            // Hide if: is the author OR already reposted
            val isAuthor = currentUserId != null && currentUserId == post.author.id
            val alreadyReposted = post.userHasReposted == true

            if (isLoggedIn && !isAuthor && !alreadyReposted) {
                btnRepost.visibility = View.VISIBLE
                txtReposted.visibility = View.GONE
                btnRepost.setOnClickListener { onRepostClick?.invoke(post.id) }
            } else if (isLoggedIn && !isAuthor && alreadyReposted) {
                btnRepost.visibility = View.GONE
                txtReposted.visibility = View.VISIBLE
            } else {
                btnRepost.visibility = View.GONE
                txtReposted.visibility = View.GONE
            }

            if (isLoggedIn && isAuthor && onDeleteClick != null) {
                btnDeletePost.visibility = View.VISIBLE
                btnDeletePost.setOnClickListener { onDeleteClick.invoke(post.id) }
            } else {
                btnDeletePost.visibility = View.GONE
            }

            itemView.setOnClickListener {
                if (onPostClick != null) {
                    onPostClick.invoke(post.id)
                } else {
                    val context = itemView.context
                    val intent = Intent(context, PostDetailActivity::class.java)
                    intent.putExtra("post_id", post.id)
                    context.startActivity(intent)
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
}
