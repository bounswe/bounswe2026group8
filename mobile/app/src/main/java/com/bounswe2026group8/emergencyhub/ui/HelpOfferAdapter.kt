package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.google.android.material.button.MaterialButton
import com.bounswe2026group8.emergencyhub.util.BadgeUtils
import com.bounswe2026group8.emergencyhub.util.TimeUtils

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
        holder.txtCategory.text = BadgeUtils.formatLabel(item.category)
        val (catText, catBg) = BadgeUtils.categoryColors(item.category)
        holder.txtCategory.setTextColor(ContextCompat.getColor(ctx, catText))
        holder.txtCategory.background.mutate().setTint(ContextCompat.getColor(ctx, catBg))

        // Availability badge
        holder.txtAvailability.text = item.availability

        // Description excerpt
        holder.txtDescription.text = item.description

        // Author
        holder.txtAuthor.text = "Offered by ${item.author.fullName}"

        // Relative timestamp
        holder.txtTimeAgo.text = TimeUtils.timeAgo(item.createdAt)

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

}
