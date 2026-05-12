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
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.util.TimeUtils
import com.bumptech.glide.Glide

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_card, parent, false)
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
            val ctx = itemView.context

            if (post.repostedFrom != null) {
                txtRepostLabel.text = ctx.getString(R.string.forum_reposted_by_format, post.author.fullName)
                txtRepostLabel.visibility = View.VISIBLE
                txtRepostLabel.setOnClickListener {
                    PublicProfileActivity.navigate(ctx, post.author.id, currentUserId)
                }
                txtAuthor.text = post.repostedFrom.author.fullName
                txtAuthor.setOnClickListener {
                    PublicProfileActivity.navigate(ctx, post.repostedFrom.author.id, currentUserId)
                }
            } else {
                txtRepostLabel.visibility = View.GONE
                txtRepostLabel.setOnClickListener(null)
                txtAuthor.text = post.author.fullName
                txtAuthor.setOnClickListener {
                    PublicProfileActivity.navigate(ctx, post.author.id, currentUserId)
                }
            }

            txtUpvote.text = "\u25B2 ${post.upvoteCount}"
            txtDownvote.text = "\u25BC ${post.downvoteCount}"
            txtTitle.text = post.title
            txtTime.text = TimeUtils.timeAgo(post.createdAt)

            val images = post.imageUrls
            if (!images.isNullOrEmpty()) {
                imageContainer.visibility = View.VISIBLE
                val thumbs = listOf(imgThumb1, imgThumb2, imgThumb3)
                for ((i, thumb) in thumbs.withIndex()) {
                    if (i < images.size) {
                        thumb.visibility = View.VISIBLE
                        Glide.with(ctx).load(RetrofitClient.resolveImageUrl(images[i])).centerCrop().into(thumb)
                    } else {
                        thumb.visibility = View.GONE
                    }
                }
                txtMoreImages.visibility = if (images.size > 3) View.VISIBLE else View.GONE
                if (images.size > 3) txtMoreImages.text = "+${images.size - 3}"
            } else {
                imageContainer.visibility = View.GONE
            }

            val commentWord = if (post.commentCount == 1) ctx.getString(R.string.comment_singular) else ctx.getString(R.string.comment_plural)
            txtComments.text = ctx.getString(R.string.comment_count, post.commentCount, commentWord)

            if (post.repostedFrom == null && post.repostCount > 0) {
                val repostWord = if (post.repostCount == 1) ctx.getString(R.string.repost_singular) else ctx.getString(R.string.repost_plural)
                txtRepostCount.text = ctx.getString(R.string.forum_repost_count_format, post.repostCount, repostWord)
                txtRepostCount.visibility = View.VISIBLE
            } else {
                txtRepostCount.visibility = View.GONE
            }

            when (post.forumType) {
                "GLOBAL" -> {
                    txtForumType.text = ctx.getString(R.string.forum_type_global)
                    txtForumType.setTextColor(ctx.getColor(R.color.forum_global))
                    txtForumType.setBackgroundResource(R.drawable.forum_type_badge_global)
                    txtForumType.visibility = View.VISIBLE
                }
                "STANDARD" -> {
                    txtForumType.text = ctx.getString(R.string.forum_type_standard)
                    txtForumType.setTextColor(ctx.getColor(R.color.forum_standard))
                    txtForumType.setBackgroundResource(R.drawable.forum_type_badge_standard)
                    txtForumType.visibility = View.VISIBLE
                }
                "URGENT" -> {
                    txtForumType.text = ctx.getString(R.string.forum_type_urgent)
                    txtForumType.setTextColor(ctx.getColor(R.color.forum_urgent))
                    txtForumType.setBackgroundResource(R.drawable.forum_type_badge_urgent)
                    txtForumType.visibility = View.VISIBLE
                }
                else -> txtForumType.visibility = View.GONE
            }

            txtUpvote.setTextColor(if (post.userVote == "UP") ctx.getColor(R.color.vote_up_active) else ctx.getColor(R.color.text_muted))
            txtDownvote.setTextColor(if (post.userVote == "DOWN") ctx.getColor(R.color.vote_down_active) else ctx.getColor(R.color.text_muted))

            txtUpvote.setOnClickListener { onVoteClick?.invoke(post.id, "UP") }
            txtDownvote.setOnClickListener { onVoteClick?.invoke(post.id, "DOWN") }

            btnShare.setOnClickListener {
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    ctx.getString(R.string.forum_post_link_label),
                    "https://emergencyhub.app/forum/posts/${post.id}"
                )
                clipboard.setPrimaryClip(clip)
                btnShare.text = ctx.getString(R.string.copied)
                btnShare.postDelayed({ btnShare.text = ctx.getString(R.string.share) }, 1500)
            }

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
                    val intent = Intent(ctx, PostDetailActivity::class.java)
                    intent.putExtra("post_id", post.id)
                    ctx.startActivity(intent)
                }
            }
        }
    }
}
