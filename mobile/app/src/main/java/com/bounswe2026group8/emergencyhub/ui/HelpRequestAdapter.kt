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

        holder.txtTitle.text = item.title

        holder.txtCategory.text = BadgeUtils.formatCategoryLabel(ctx, item.category)
        val (catText, catBg) = BadgeUtils.categoryColors(item.category)
        holder.txtCategory.setTextColor(ContextCompat.getColor(ctx, catText))
        holder.txtCategory.background.mutate().setTint(ContextCompat.getColor(ctx, catBg))

        holder.txtUrgency.text = BadgeUtils.formatUrgencyLabel(ctx, item.urgency)
        val (urgText, urgBg) = BadgeUtils.urgencyColors(item.urgency)
        holder.txtUrgency.setTextColor(ContextCompat.getColor(ctx, urgText))
        holder.txtUrgency.background.mutate().setTint(ContextCompat.getColor(ctx, urgBg))

        holder.txtStatus.text = BadgeUtils.formatStatusLabel(ctx, item.status)
        holder.txtAuthor.text = item.author.fullName
        holder.txtCommentCount.text = "\uD83D\uDCAC ${item.commentCount}"
        holder.txtTimeAgo.text = TimeUtils.timeAgo(item.createdAt)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<HelpRequestItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
