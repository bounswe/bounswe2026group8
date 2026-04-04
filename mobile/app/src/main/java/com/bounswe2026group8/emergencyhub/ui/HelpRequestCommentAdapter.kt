package com.bounswe2026group8.emergencyhub.ui

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpRequestComment
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * RecyclerView adapter for the comment list on the help-request detail screen.
 *
 * Expert authors get a visible badge and their expertise field shown.
 * The comment author gets a Delete button; [onDeleteClick] is invoked on tap
 * and the caller is responsible for the actual API call and list update.
 */
class HelpRequestCommentAdapter(
    private var items: List<HelpRequestComment>,
    private val currentUserId: Int? = null,
    private val onDeleteClick: (HelpRequestComment) -> Unit = {},
) : RecyclerView.Adapter<HelpRequestCommentAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCommentAuthor: TextView = view.findViewById(R.id.txtCommentAuthor)
        val txtExpertBadge: TextView = view.findViewById(R.id.txtExpertBadge)
        val txtExpertiseField: TextView = view.findViewById(R.id.txtExpertiseField)
        val txtCommentContent: TextView = view.findViewById(R.id.txtCommentContent)
        val txtCommentTime: TextView = view.findViewById(R.id.txtCommentTime)
        val btnDeleteComment: TextView = view.findViewById(R.id.btnDeleteComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_help_request_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = items[position]

        holder.txtCommentAuthor.text = comment.author.fullName
        holder.txtCommentContent.text = comment.content
        holder.txtCommentTime.text = formatTimeAgo(comment.createdAt)

        // Expert badge + expertise field
        if (comment.author.role == "EXPERT") {
            holder.txtExpertBadge.visibility = View.VISIBLE
            if (!comment.author.expertiseField.isNullOrBlank()) {
                holder.txtExpertiseField.text = comment.author.expertiseField
                holder.txtExpertiseField.visibility = View.VISIBLE
            } else {
                holder.txtExpertiseField.visibility = View.GONE
            }
        } else {
            holder.txtExpertBadge.visibility = View.GONE
            holder.txtExpertiseField.visibility = View.GONE
        }

        // Delete button — only visible to the comment author
        if (currentUserId != null && comment.author.id == currentUserId) {
            holder.btnDeleteComment.visibility = View.VISIBLE
            holder.btnDeleteComment.setOnClickListener { onDeleteClick(comment) }
        } else {
            holder.btnDeleteComment.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    /** Replaces the full dataset. */
    fun updateItems(newItems: List<HelpRequestComment>) {
        items = newItems
        notifyDataSetChanged()
    }

    /** Appends a single comment (used after posting). */
    fun addComment(comment: HelpRequestComment) {
        items = items + comment
        notifyItemInserted(items.size - 1)
    }

    /** Removes a single comment by ID (used after deletion). */
    fun removeComment(commentId: Int) {
        val index = items.indexOfFirst { it.id == commentId }
        if (index != -1) {
            items = items.toMutableList().also { it.removeAt(index) }
            notifyItemRemoved(index)
        }
    }

    private fun formatTimeAgo(iso: String): String {
        return try {
            val trimmed = iso.substringBefore(".").substringBefore("Z").substringBefore("+")
            val millis = dateFormat.parse(trimmed)?.time ?: return iso
            DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (_: Exception) {
            iso
        }
    }
}
