package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Hub
import com.google.android.material.button.MaterialButton

class HubAdapter(
    private val onRename: (Hub) -> Unit,
    private val onDelete: (Hub) -> Unit,
) : RecyclerView.Adapter<HubAdapter.ViewHolder>() {

    private val items = mutableListOf<Hub>()

    fun submit(newItems: List<Hub>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun add(hub: Hub) {
        items.add(hub)
        notifyItemInserted(items.size - 1)
    }

    fun replace(updated: Hub) {
        val index = items.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            items[index] = updated
            notifyItemChanged(index)
        }
    }

    fun remove(hubId: Int) {
        val index = items.indexOfFirst { it.id == hubId }
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hub, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtName: TextView = view.findViewById(R.id.txtHubName)
        private val txtSlug: TextView = view.findViewById(R.id.txtHubSlug)
        private val btnRename: MaterialButton = view.findViewById(R.id.btnRenameHub)
        private val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteHub)

        fun bind(hub: Hub) {
            txtName.text = hub.name
            txtSlug.text = hub.slug
            btnRename.setOnClickListener { onRename(hub) }
            btnDelete.setOnClickListener { onDelete(hub) }
        }
    }
}
