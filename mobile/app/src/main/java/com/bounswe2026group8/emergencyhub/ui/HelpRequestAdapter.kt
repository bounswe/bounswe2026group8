package com.bounswe2026group8.emergencyhub.ui

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    /** Parses ISO 8601 timestamps from the backend. */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

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
        holder.txtCategory.text = formatCategory(item.category)
        val (catText, catBg) = categoryColors(item.category)
        holder.txtCategory.setTextColor(ContextCompat.getColor(ctx, catText))
        holder.txtCategory.background.mutate().setTint(ContextCompat.getColor(ctx, catBg))

        // Urgency badge — colored pill
        holder.txtUrgency.text = formatUrgency(item.urgency)
        val (urgText, urgBg) = urgencyColors(item.urgency)
        holder.txtUrgency.setTextColor(ContextCompat.getColor(ctx, urgText))
        holder.txtUrgency.background.mutate().setTint(ContextCompat.getColor(ctx, urgBg))

        // Status badge
        holder.txtStatus.text = formatStatus(item.status)

        // Author
        holder.txtAuthor.text = item.author.fullName

        // Comment count
        holder.txtCommentCount.text = "\uD83D\uDCAC ${item.commentCount}"

        // Relative timestamp
        holder.txtTimeAgo.text = formatTimeAgo(item.createdAt)

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

    private fun formatCategory(raw: String): String = raw.lowercase()
        .replaceFirstChar { it.uppercase() }

    private fun formatUrgency(raw: String): String = raw.lowercase()
        .replaceFirstChar { it.uppercase() }

    private fun formatStatus(raw: String): String = when (raw) {
        "EXPERT_RESPONDING" -> "Expert Responding"
        "RESOLVED" -> "Resolved"
        else -> "Open"
    }

    /** Returns (textColor, backgroundColor) resource IDs for a category. */
    private fun categoryColors(category: String): Pair<Int, Int> = when (category) {
        "MEDICAL"   -> R.color.category_medical   to R.color.category_medical_bg
        "FOOD"      -> R.color.category_food       to R.color.category_food_bg
        "SHELTER"   -> R.color.category_shelter    to R.color.category_shelter_bg
        "TRANSPORT" -> R.color.category_transport  to R.color.category_transport_bg
        else        -> R.color.text_secondary      to R.color.badge_muted_bg
    }

    /** Returns (textColor, backgroundColor) resource IDs for an urgency level. */
    private fun urgencyColors(urgency: String): Pair<Int, Int> = when (urgency) {
        "HIGH"   -> R.color.urgency_high   to R.color.urgency_high_bg
        "MEDIUM" -> R.color.urgency_medium to R.color.urgency_medium_bg
        else     -> R.color.urgency_low    to R.color.urgency_low_bg
    }

    /**
     * Converts an ISO 8601 timestamp to a human-readable relative string
     * (e.g. "5 minutes ago", "2 hours ago").
     */
    private fun formatTimeAgo(iso: String): String {
        return try {
            // Strip fractional seconds and timezone suffix for SimpleDateFormat
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
