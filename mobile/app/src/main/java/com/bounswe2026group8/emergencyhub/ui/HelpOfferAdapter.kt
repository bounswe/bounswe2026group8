package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.bounswe2026group8.emergencyhub.util.BadgeUtils
import com.bounswe2026group8.emergencyhub.util.TimeUtils
import com.google.android.material.button.MaterialButton

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
        holder.txtCategory.text = BadgeUtils.formatCategoryLabel(ctx, item.category)
        val (catText, catBg) = BadgeUtils.categoryColors(item.category)
        holder.txtCategory.setTextColor(ContextCompat.getColor(ctx, catText))
        holder.txtCategory.background.mutate().setTint(ContextCompat.getColor(ctx, catBg))

        holder.txtAvailability.text = BadgeUtils.formatAvailabilityLabel(ctx, item.availability)
        holder.txtDescription.text = item.description
        holder.txtAuthor.text = ctx.getString(R.string.help_offer_author_format, item.author.fullName)
        holder.txtAuthor.setOnClickListener {
            PublicProfileActivity.navigate(ctx, item.author.id, currentUserId)
        }
        holder.txtTimeAgo.text = TimeUtils.timeAgo(item.createdAt)

        if (currentUserId != null && item.author.id == currentUserId) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<HelpOfferItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun removeItem(offerId: Int) {
        val index = items.indexOfFirst { it.id == offerId }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
