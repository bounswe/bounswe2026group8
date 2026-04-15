package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import com.bounswe2026group8.emergencyhub.util.BadgeUtils
import com.bounswe2026group8.emergencyhub.util.TimeUtils

/**
 * RecyclerView adapter for the help-requests list.
 *
 * Each item displays title, category/urgency/status badges,
 * author name, comment count, and relative timestamp.
 * Tapping an item triggers [onItemClick].
 */
class HelpRequestAdapter(
    private var items: List<HelpRequestItem>,
    private val onItemClick: (HelpRequestItem) -> Unit
) : RecyclerView.Adapter<HelpRequestAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtCategory: TextView = view.findViewById(R.id.txtCategory)
        val txtUrgency: TextView = view.findViewById(R.id.txtUrgency)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val txtAuthor: TextView = view.findViewById(R.id.txtAuthor)
        val txtCommentCount: TextView = view.findViewById(R.id.txtCommentCount)
        val txtTimeAgo: TextView = view.findViewById(R.id.txtTimeAgo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_help_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        // Title
        holder.txtTitle.text = item.title

        // Category badge — colored pill
        holder.txtCategory.text = BadgeUtils.formatLabel(item.category)
        val (catText, catBg) = BadgeUtils.categoryColors(item.category)
        holder.txtCategory.setTextColor(ContextCompat.getColor(ctx, catText))
        holder.txtCategory.background.mutate().setTint(ContextCompat.getColor(ctx, catBg))

        // Urgency badge — colored pill
        holder.txtUrgency.text = BadgeUtils.formatLabel(item.urgency)
        val (urgText, urgBg) = BadgeUtils.urgencyColors(item.urgency)
        holder.txtUrgency.setTextColor(ContextCompat.getColor(ctx, urgText))
        holder.txtUrgency.background.mutate().setTint(ContextCompat.getColor(ctx, urgBg))

        // Status badge
        holder.txtStatus.text = formatStatus(item.status)

        // Author
        holder.txtAuthor.text = item.author.fullName

        // Comment count
        holder.txtCommentCount.text = "\uD83D\uDCAC ${item.commentCount}"

        // Relative timestamp
        holder.txtTimeAgo.text = TimeUtils.timeAgo(item.createdAt)

        // Click handler
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    /** Replaces the dataset and refreshes the list. */
    fun updateItems(newItems: List<HelpRequestItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    // ── Formatting helpers ───────────────────────────────────────────────

    private fun formatStatus(raw: String): String = when (raw) {
        "EXPERT_RESPONDING" -> "Expert Responding"
        "RESOLVED" -> "Resolved"
        else -> "Open"
    }
}
