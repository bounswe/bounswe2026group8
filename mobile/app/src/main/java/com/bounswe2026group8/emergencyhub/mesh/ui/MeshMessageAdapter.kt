package com.bounswe2026group8.emergencyhub.mesh.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MeshMessageAdapter(
    private var messages: List<MeshMessage> = emptyList()
) : RecyclerView.Adapter<MeshMessageAdapter.ViewHolder>() {

    fun submitList(newMessages: List<MeshMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mesh_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtAuthor: TextView = itemView.findViewById(R.id.txtAuthor)
        private val txtHops: TextView = itemView.findViewById(R.id.txtHops)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val txtBody: TextView = itemView.findViewById(R.id.txtBody)

        fun bind(message: MeshMessage) {
            txtAuthor.text = message.authorDisplayName
                ?: "device-${message.authorDeviceId}"
            txtHops.text = if (message.hopCount == 0) "direct" else "${message.hopCount} hop(s)"
            txtTime.text = formatTimestamp(message.createdAt)
            txtBody.text = message.body
        }

        private fun formatTimestamp(epochMillis: Long): String {
            val now = Calendar.getInstance()
            val msg = Calendar.getInstance().apply { timeInMillis = epochMillis }
            val sameDay = now.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR)
            val pattern = if (sameDay) "HH:mm:ss" else "MMM d, HH:mm"
            return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epochMillis))
        }
    }
}
