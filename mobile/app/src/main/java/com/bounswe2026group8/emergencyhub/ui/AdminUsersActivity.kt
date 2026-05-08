package com.bounswe2026group8.emergencyhub.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.AccountStatusUpdateRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.StaffRoleUpdateRequest
import com.bounswe2026group8.emergencyhub.api.StaffUserListItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Admin-only screen: list, search, change staff roles, and toggle activation.
 *
 * Backend enforces the actual permissions; this UI is gated by [StaffRoleHelper]
 * via [StaffDashboardActivity].
 */
class AdminUsersActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var txtStatus: TextView
    private lateinit var inputSearch: TextInputEditText
    private lateinit var adapter: AdminUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerUsers)
        txtStatus = findViewById(R.id.txtStatus)
        inputSearch = findViewById(R.id.inputSearch)

        adapter = AdminUserAdapter(
            onChangeRole = { user -> showChangeRoleDialog(user) },
            onToggleStatus = { user -> showToggleStatusDialog(user) },
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.btnRefresh).setOnClickListener { loadUsers() }
        inputSearch.setOnEditorActionListener { _, _, _ ->
            loadUsers(); true
        }

        loadUsers()
    }

    private fun loadUsers() {
        val query = inputSearch.text?.toString()?.trim().orEmpty()
        showStatus(getString(R.string.staff_loading))
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@AdminUsersActivity)
                    .listStaffUsers(search = query.ifBlank { null })
                if (res.isSuccessful) {
                    val items = res.body().orEmpty()
                    adapter.submit(items)
                    if (items.isEmpty()) {
                        showStatus(getString(R.string.staff_no_results))
                    } else {
                        hideStatus()
                    }
                } else {
                    showStatus(getString(R.string.staff_request_failed))
                }
            } catch (_: Exception) {
                showStatus(getString(R.string.staff_request_failed))
            }
        }
    }

    private fun showStatus(message: String) {
        txtStatus.text = message
        txtStatus.visibility = View.VISIBLE
    }

    private fun hideStatus() {
        txtStatus.visibility = View.GONE
    }

    private fun showChangeRoleDialog(user: StaffUserListItem) {
        val labels = arrayOf(
            getString(R.string.staff_role_none),
            getString(R.string.staff_role_moderator),
            getString(R.string.staff_role_verification_coordinator),
            getString(R.string.staff_role_admin),
        )
        val values = arrayOf(
            StaffRoleHelper.NONE,
            StaffRoleHelper.MODERATOR,
            StaffRoleHelper.VERIFICATION_COORDINATOR,
            StaffRoleHelper.ADMIN,
        )
        val current = values.indexOf(user.staffRole).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.staff_action_change_role))
            .setSingleChoiceItems(labels, current) { dialog, which ->
                dialog.dismiss()
                val newRole = values[which]
                if (newRole == user.staffRole) return@setSingleChoiceItems
                promptForReason(
                    title = "Reason (optional)",
                    required = false,
                ) { reason ->
                    submitRoleChange(user, newRole, reason)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun submitRoleChange(user: StaffUserListItem, newRole: String, reason: String?) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@AdminUsersActivity).updateStaffRole(
                    userId = user.id,
                    body = StaffRoleUpdateRequest(staffRole = newRole, reason = reason),
                )
                if (res.isSuccessful) {
                    Toast.makeText(
                        this@AdminUsersActivity,
                        R.string.staff_action_succeeded,
                        Toast.LENGTH_SHORT,
                    ).show()
                    res.body()?.let { adapter.replace(it) }
                } else {
                    Toast.makeText(
                        this@AdminUsersActivity,
                        R.string.staff_request_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@AdminUsersActivity, R.string.staff_request_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showToggleStatusDialog(user: StaffUserListItem) {
        val willSuspend = user.isActive
        val title =
            if (willSuspend) getString(R.string.staff_action_suspend)
            else getString(R.string.staff_action_reactivate)
        promptForReason(title = title, required = true) { reason ->
            if (reason.isNullOrBlank()) {
                Toast.makeText(this, R.string.staff_reason_required, Toast.LENGTH_SHORT).show()
                return@promptForReason
            }
            lifecycleScope.launch {
                try {
                    val res = RetrofitClient.getService(this@AdminUsersActivity).updateAccountStatus(
                        userId = user.id,
                        body = AccountStatusUpdateRequest(isActive = !willSuspend, reason = reason),
                    )
                    if (res.isSuccessful) {
                        Toast.makeText(
                            this@AdminUsersActivity,
                            R.string.staff_action_succeeded,
                            Toast.LENGTH_SHORT,
                        ).show()
                        res.body()?.let { adapter.replace(it) }
                    } else {
                        Toast.makeText(
                            this@AdminUsersActivity,
                            R.string.staff_request_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        this@AdminUsersActivity,
                        R.string.staff_request_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun promptForReason(
        title: String,
        required: Boolean,
        onSubmit: (String?) -> Unit,
    ) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        input.setSingleLine(false)
        input.setHint("Reason")
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text?.toString()?.trim().orEmpty()
                onSubmit(if (text.isBlank() && !required) null else text)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
