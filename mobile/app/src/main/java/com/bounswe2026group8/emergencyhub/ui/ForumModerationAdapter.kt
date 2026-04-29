package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.ForumModerationPost
import com.google.android.material.button.MaterialButton

class ForumModerationAdapter(
    private val onAction: (post: ForumModerationPost, action: String) -> Unit,
) : RecyclerView.Adapter<ForumModerationAdapter.ViewHolder>() {

    private val items = mutableListOf<ForumModerationPost>()

    fun submit(newItems: List<ForumModerationPost>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_moderation_post, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        private val txtMeta: TextView = view.findViewById(R.id.txtMeta)
        private val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        private val btnHide: MaterialButton = view.findViewById(R.id.btnHide)
        private val btnRestore: MaterialButton = view.findViewById(R.id.btnRestore)
        private val btnRemove: MaterialButton = view.findViewById(R.id.btnRemove)

        fun bind(post: ForumModerationPost) {
            txtTitle.text = post.title
            txtStatus.text = post.status

            val parts = buildList {
                post.author?.email?.let { add("by $it") }
                post.hubName?.let { add(it) }
                post.forumType?.let { add(it) }
                add("${post.commentCount} comments")
            }
            txtMeta.text = parts.joinToString(" · ")

            btnHide.visibility = if (post.status != "HIDDEN") View.VISIBLE else View.GONE
            btnRestore.visibility = if (post.status != "ACTIVE") View.VISIBLE else View.GONE
            btnRemove.visibility = if (post.status != "REMOVED") View.VISIBLE else View.GONE

            btnHide.setOnClickListener { onAction(post, "HIDE") }
            btnRestore.setOnClickListener { onAction(post, "RESTORE") }
            btnRemove.setOnClickListener { onAction(post, "REMOVE") }
        }
    }
}
