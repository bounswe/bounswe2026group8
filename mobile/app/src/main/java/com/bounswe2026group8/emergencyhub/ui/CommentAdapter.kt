package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Comment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CommentAdapter(
    private var comments: MutableList<Comment> = mutableListOf(),
    private val currentUserId: Int?,
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    fun submitList(newComments: List<Comment>) {
        comments = newComments.toMutableList()
        notifyDataSetChanged()
    }

    fun addComment(comment: Comment) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }

    fun removeComment(commentId: Int) {
        val index = comments.indexOfFirst { it.id == commentId }
        if (index != -1) {
            comments.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position], currentUserId, onDeleteClick)
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtAuthor: TextView = itemView.findViewById(R.id.txtCommentAuthor)
        private val txtTime: TextView = itemView.findViewById(R.id.txtCommentTime)
        private val txtBody: TextView = itemView.findViewById(R.id.txtCommentBody)
        private val btnDelete: TextView = itemView.findViewById(R.id.btnDeleteComment)

        fun bind(comment: Comment, currentUserId: Int?, onDeleteClick: (Comment) -> Unit) {
            txtAuthor.text = comment.author.fullName
            txtTime.text = timeAgo(comment.createdAt)
            txtBody.text = comment.content

            if (currentUserId != null && currentUserId == comment.author.id) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDeleteClick(comment) }
            } else {
                btnDelete.visibility = View.GONE
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
