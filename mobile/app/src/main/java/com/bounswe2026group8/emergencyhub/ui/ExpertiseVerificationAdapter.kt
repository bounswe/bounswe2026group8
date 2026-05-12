package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.ExpertiseVerificationItem
import com.google.android.material.button.MaterialButton

class ExpertiseVerificationAdapter(
    private val onDecision: (item: ExpertiseVerificationItem, decision: String) -> Unit,
) : RecyclerView.Adapter<ExpertiseVerificationAdapter.ViewHolder>() {

    private val items = mutableListOf<ExpertiseVerificationItem>()

    fun submit(newItems: List<ExpertiseVerificationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expertise_verification, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtField: TextView = view.findViewById(R.id.txtField)
        private val txtMeta: TextView = view.findViewById(R.id.txtMeta)
        private val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        private val txtNote: TextView = view.findViewById(R.id.txtNote)
        private val btnApprove: MaterialButton = view.findViewById(R.id.btnApprove)
        private val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        private val btnReopen: MaterialButton = view.findViewById(R.id.btnReopen)

        fun bind(item: ExpertiseVerificationItem) {
            txtField.text = item.field
            txtStatus.text = item.verificationStatus

            val parts = buildList {
                item.user?.email?.let { add(it) }
                add(item.certificationLevel)
                item.reviewedByName?.let { add("reviewed by $it") }
            }
            txtMeta.text = parts.joinToString(" · ")

            if (!item.verificationNote.isNullOrBlank()) {
                txtNote.text = "Note: ${item.verificationNote}"
                txtNote.visibility = View.VISIBLE
            } else {
                txtNote.visibility = View.GONE
            }

            btnApprove.visibility = if (item.verificationStatus != "APPROVED") View.VISIBLE else View.GONE
            btnReject.visibility = if (item.verificationStatus != "REJECTED") View.VISIBLE else View.GONE
            btnReopen.visibility = if (item.verificationStatus != "PENDING") View.VISIBLE else View.GONE

            btnApprove.setOnClickListener { onDecision(item, "APPROVED") }
            btnReject.setOnClickListener { onDecision(item, "REJECTED") }
            btnReopen.setOnClickListener { onDecision(item, "PENDING") }
        }
    }
}
