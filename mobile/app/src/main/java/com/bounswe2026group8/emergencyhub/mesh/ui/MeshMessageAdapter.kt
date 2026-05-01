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

/**
 * Adapter for the mesh forum's post list. Each row is a top-level post
 * (parentPostId == null). Tap a row to open the post detail screen.
 */
class MeshMessageAdapter(
    private var posts: List<MeshMessage> = emptyList(),
    private var commentCounts: Map<String, Int> = emptyMap(),
    private var onPostClick: ((MeshMessage) -> Unit)? = null
) : RecyclerView.Adapter<MeshMessageAdapter.ViewHolder>() {

    fun submitList(
        newPosts: List<MeshMessage>,
        newCommentCounts: Map<String, Int>
    ) {
        posts = newPosts
        commentCounts = newCommentCounts
        notifyDataSetChanged()
    }

    fun setOnPostClick(listener: (MeshMessage) -> Unit) {
        onPostClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mesh_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post, commentCounts[post.id] ?: 0)
        holder.itemView.setOnClickListener { onPostClick?.invoke(post) }
    }

    override fun getItemCount(): Int = posts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtPostType: TextView = itemView.findViewById(R.id.txtPostType)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        private val txtBody: TextView = itemView.findViewById(R.id.txtBody)
        private val txtLocation: TextView = itemView.findViewById(R.id.txtLocation)
        private val txtAuthor: TextView = itemView.findViewById(R.id.txtAuthor)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val txtCommentCount: TextView = itemView.findViewById(R.id.txtCommentCount)

        fun bind(post: MeshMessage, commentCount: Int) {
            renderPostType(post.postType)
            txtTitle.text = post.title.orEmpty()
            txtBody.text = post.body
            renderLocation(post)
            txtAuthor.text = post.authorDisplayName
                ?: "device-${post.authorDeviceId}"
            txtTime.text = formatTimestamp(post.createdAt)
            txtCommentCount.text = itemView.context.getString(
                R.string.mesh_comment_count_format, commentCount
            )
        }

        private fun renderPostType(type: String?) {
            val ctx = itemView.context
            when (type) {
                "NEED_HELP" -> {
                    txtPostType.text = ctx.getString(R.string.mesh_post_type_need)
                    txtPostType.setTextColor(ctx.getColor(R.color.urgency_high))
                    txtPostType.setBackgroundColor(ctx.getColor(R.color.urgency_high_bg))
                    txtPostType.visibility = View.VISIBLE
                }
                "OFFER_HELP" -> {
                    txtPostType.text = ctx.getString(R.string.mesh_post_type_offer)
                    txtPostType.setTextColor(ctx.getColor(R.color.urgency_low))
                    txtPostType.setBackgroundColor(ctx.getColor(R.color.urgency_low_bg))
                    txtPostType.visibility = View.VISIBLE
                }
                else -> txtPostType.visibility = View.GONE
            }
        }

        private fun renderLocation(post: MeshMessage) {
            val lat = post.latitude
            val lon = post.longitude
            if (lat == null || lon == null) {
                txtLocation.visibility = View.GONE
                return
            }
            txtLocation.visibility = View.VISIBLE
            txtLocation.text = formatLocation(lat, lon, post.locAccuracyMeters, post.locCapturedAt)
        }

        private fun formatLocation(
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
                val age = when {
                    ageSec < 60 -> "${ageSec}s ago"
                    ageSec < 3600 -> "${ageSec / 60}m ago"
                    ageSec < 86400 -> "${ageSec / 3600}h ago"
                    else -> "${ageSec / 86400}d ago"
                }
                parts += "fix $age"
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
