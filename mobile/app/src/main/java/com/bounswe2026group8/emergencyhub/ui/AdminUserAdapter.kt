package com.bounswe2026group8.emergencyhub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.StaffUserListItem
import com.google.android.material.button.MaterialButton

/**
 * RecyclerView adapter for the admin user-management list.
 */
class AdminUserAdapter(
    private val onChangeRole: (StaffUserListItem) -> Unit,
    private val onToggleStatus: (StaffUserListItem) -> Unit,
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    private val items = mutableListOf<StaffUserListItem>()

    fun submit(newItems: List<StaffUserListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun replace(updated: StaffUserListItem) {
        val index = items.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            items[index] = updated
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtName: TextView = view.findViewById(R.id.txtName)
        private val txtEmail: TextView = view.findViewById(R.id.txtEmail)
        private val txtRole: TextView = view.findViewById(R.id.txtRole)
        private val txtStaffRole: TextView = view.findViewById(R.id.txtStaffRole)
        private val txtActive: TextView = view.findViewById(R.id.txtActive)
        private val btnChangeRole: MaterialButton = view.findViewById(R.id.btnChangeRole)
        private val btnToggleStatus: MaterialButton = view.findViewById(R.id.btnToggleStatus)

        fun bind(user: StaffUserListItem) {
            txtName.text = user.fullName
            txtEmail.text = user.email
            txtRole.text = if (user.role == "EXPERT") "🎓 Expert" else "👤 Standard"
            txtStaffRole.text = StaffRoleHelper.label(user.staffRole)
            txtActive.text = if (user.isActive) "Active" else "Suspended"

            btnToggleStatus.text = itemView.context.getString(
                if (user.isActive) R.string.staff_action_suspend
                else R.string.staff_action_reactivate
            )

            btnChangeRole.setOnClickListener { onChangeRole(user) }
            btnToggleStatus.setOnClickListener { onToggleStatus(user) }
        }
    }
}
