package com.bounswe2026group8.emergencyhub.ui

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * RecyclerView adapter for the help-offers list.
 *
 * Each card shows skill/resource title, category + availability badges,
 * description excerpt, author name, and relative timestamp.
 * The delete button is only visible when the logged-in user is the author.
 */
class HelpOfferAdapter(
    private var items: MutableList<HelpOfferItem>,
    private val currentUserId: Int?,
    private val onItemClick: (HelpOfferItem) -> Unit,
    private val onDeleteClick: (HelpOfferItem) -> Unit
) : RecyclerView.Adapter<HelpOfferAdapter.ViewHolder>() {

    /** Parses ISO 8601 timestamps from the backend. */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtSkillOrResource: TextView = view.findViewById(R.id.txtSkillOrResource)
        val txtCategory: TextView = view.findViewById(R.id.txtCategory)
        val txtAvailability: TextView = view.findViewById(R.id.txtAvailability)
        val txtDescription: TextView = view.findViewById(R.id.txtDescription)
        val txtAuthor: TextView = view.findViewById(R.id.txtAuthor)
        val txtTimeAgo: TextView = view.findViewById(R.id.txtTimeAgo)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_help_offer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        holder.txtSkillOrResource.text = item.skillOrResource

        // Category badge — colored pill
        holder.txtCategory.text = formatCategory(item.category)
        val (catText, catBg) = categoryColors(item.category)
        holder.txtCategory.setTextColor(ContextCompat.getColor(ctx, catText))
        holder.txtCategory.background.mutate().setTint(ContextCompat.getColor(ctx, catBg))

        // Availability badge
        holder.txtAvailability.text = item.availability

        // Description excerpt
        holder.txtDescription.text = item.description

        // Author
        holder.txtAuthor.text = "Offered by ${item.author.fullName}"

        // Relative timestamp
        holder.txtTimeAgo.text = formatTimeAgo(item.createdAt)

        // Delete button — only visible for the author's own offers
        if (currentUserId != null && item.author.id == currentUserId) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }

        // Card click
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    /** Replaces the full dataset and refreshes the list. */
    fun updateItems(newItems: List<HelpOfferItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    /** Removes a single offer by ID with an animated removal. */
    fun removeItem(offerId: Int) {
        val index = items.indexOfFirst { it.id == offerId }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    // ── Formatting helpers ───────────────────────────────────────────────

    private fun formatCategory(raw: String): String = raw.lowercase()
        .replaceFirstChar { it.uppercase() }

    /** Returns (textColor, backgroundColor) resource IDs for a category. */
    private fun categoryColors(category: String): Pair<Int, Int> = when (category) {
        "MEDICAL"   -> R.color.category_medical   to R.color.category_medical_bg
        "FOOD"      -> R.color.category_food       to R.color.category_food_bg
        "SHELTER"   -> R.color.category_shelter    to R.color.category_shelter_bg
        "TRANSPORT" -> R.color.category_transport  to R.color.category_transport_bg
        else        -> R.color.text_secondary      to R.color.badge_muted_bg
    }

    /**
     * Converts an ISO 8601 timestamp to a human-readable relative string
     * (e.g. "5 minutes ago", "2 hours ago").
     */
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
