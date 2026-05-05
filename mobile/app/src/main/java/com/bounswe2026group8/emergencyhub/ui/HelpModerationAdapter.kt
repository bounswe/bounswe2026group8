package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpOfferModerationItem
import com.bounswe2026group8.emergencyhub.api.HelpRequestModerationItem
import com.google.android.material.button.MaterialButton

/**
 * Single adapter that renders either a help request or a help offer row.
 *
 * The activity swaps between modes by calling [submitRequests] or
 * [submitOffers], which clears the other list internally.
 */
class HelpModerationAdapter(
    private val onDelete: (item: Any) -> Unit,
) : RecyclerView.Adapter<HelpModerationAdapter.ViewHolder>() {

    private sealed interface Row {
        data class Request(val item: HelpRequestModerationItem) : Row
        data class Offer(val item: HelpOfferModerationItem) : Row
    }

    private val items = mutableListOf<Row>()

    fun submitRequests(newItems: List<HelpRequestModerationItem>) {
        items.clear()
        newItems.forEach { items.add(Row.Request(it)) }
        notifyDataSetChanged()
    }

    fun submitOffers(newItems: List<HelpOfferModerationItem>) {
        items.clear()
        newItems.forEach { items.add(Row.Offer(it)) }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_help_moderation, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val row = items[position]) {
            is Row.Request -> holder.bindRequest(row.item)
            is Row.Offer -> holder.bindOffer(row.item)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        private val txtCategory: TextView = view.findViewById(R.id.txtCategory)
        private val txtMeta: TextView = view.findViewById(R.id.txtMeta)
        private val txtDescription: TextView = view.findViewById(R.id.txtDescription)
        private val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)

        fun bindRequest(item: HelpRequestModerationItem) {
            txtTitle.text = item.title
            setCategory(item.category)

            val parts = buildList {
                item.author?.email?.let { add("by $it") }
                item.hubName?.let { add(it) }
                item.status?.let { add(it) }
                item.urgency?.let { add(it) }
            }
            txtMeta.text = parts.joinToString(" · ")

            if (!item.description.isNullOrBlank()) {
                txtDescription.text = item.description
                txtDescription.visibility = View.VISIBLE
            } else {
                txtDescription.visibility = View.GONE
            }

            btnDelete.setOnClickListener { onDelete(item) }
        }

        fun bindOffer(item: HelpOfferModerationItem) {
            txtTitle.text = item.skillOrResource
            setCategory(item.category)

            val parts = buildList {
                item.author?.email?.let { add("by $it") }
                item.hubName?.let { add(it) }
            }
            txtMeta.text = parts.joinToString(" · ")

            if (!item.description.isNullOrBlank()) {
                txtDescription.text = item.description
                txtDescription.visibility = View.VISIBLE
            } else {
                txtDescription.visibility = View.GONE
            }

            btnDelete.setOnClickListener { onDelete(item) }
        }

        private fun setCategory(category: String?) {
            if (category.isNullOrBlank()) {
                txtCategory.visibility = View.GONE
            } else {
                txtCategory.text = category
                txtCategory.visibility = View.VISIBLE
            }
        }
    }
}
