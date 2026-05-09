package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.AuditLogItem

class AuditLogAdapter : RecyclerView.Adapter<AuditLogAdapter.ViewHolder>() {

    private val items = mutableListOf<AuditLogItem>()

    fun submit(newItems: List<AuditLogItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audit_log, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtAction: TextView = view.findViewById(R.id.txtAction)
        private val txtWhen: TextView = view.findViewById(R.id.txtWhen)
        private val txtActor: TextView = view.findViewById(R.id.txtActor)
        private val txtTarget: TextView = view.findViewById(R.id.txtTarget)
        private val txtReason: TextView = view.findViewById(R.id.txtReason)

        fun bind(item: AuditLogItem) {
            txtAction.text = item.action
            txtWhen.text = item.createdAt.orEmpty()
            txtActor.text = "by ${item.actorEmail ?: "—"}"
            val targetParts = buildList {
                item.targetType?.let { add(it) }
                item.targetId?.let { add("#$it") }
                item.targetUserEmail?.let { add("($it)") }
            }
            txtTarget.text = if (targetParts.isEmpty()) "—" else targetParts.joinToString(" ")

            if (!item.reason.isNullOrBlank()) {
                txtReason.text = "Reason: ${item.reason}"
                txtReason.visibility = View.VISIBLE
            } else {
                txtReason.visibility = View.GONE
            }
        }
    }
}
