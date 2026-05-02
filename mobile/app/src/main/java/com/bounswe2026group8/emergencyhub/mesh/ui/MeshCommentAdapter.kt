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

class MeshCommentAdapter(
    private var comments: List<MeshMessage> = emptyList()
) : RecyclerView.Adapter<MeshCommentAdapter.ViewHolder>() {

    fun submitList(newComments: List<MeshMessage>) {
        comments = newComments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mesh_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtAuthor: TextView = itemView.findViewById(R.id.txtAuthor)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val txtBody: TextView = itemView.findViewById(R.id.txtBody)
        private val txtLocation: TextView = itemView.findViewById(R.id.txtLocation)

        fun bind(comment: MeshMessage) {
            val ctx = itemView.context
            txtAuthor.text = comment.authorDisplayName
                ?: ctx.getString(R.string.mesh_device_fallback_format, comment.authorDeviceId)
            txtTime.text = formatTimestamp(comment.createdAt)
            txtBody.text = comment.body

            val lat = comment.latitude
            val lon = comment.longitude
            if (lat != null && lon != null) {
                txtLocation.visibility = View.VISIBLE
                txtLocation.text = formatLocation(
                    ctx, lat, lon, comment.locAccuracyMeters, comment.locCapturedAt
                )
            } else {
                txtLocation.visibility = View.GONE
            }
        }

        private fun formatLocation(
            ctx: android.content.Context,
            lat: Double,
            lon: Double,
            accuracyMeters: Float?,
            capturedAt: Long?
        ): String {
            val coords = String.format(Locale.US, "%.5f, %.5f", lat, lon)
            val parts = mutableListOf("📍 $coords")
            if (accuracyMeters != null) parts += "±${accuracyMeters.toInt()}m"
            if (capturedAt != null) {
                val ageSec = (System.currentTimeMillis() - capturedAt) / 1000
                parts += formatFixAge(ctx, ageSec)
            }
            return parts.joinToString(" · ")
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
