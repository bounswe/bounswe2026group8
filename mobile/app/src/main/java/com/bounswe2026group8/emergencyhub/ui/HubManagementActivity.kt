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
import com.bounswe2026group8.emergencyhub.api.Hub
import com.bounswe2026group8.emergencyhub.api.HubCreateRequest
import com.bounswe2026group8.emergencyhub.api.HubDeleteRequest
import com.bounswe2026group8.emergencyhub.api.HubUpdateRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Admin-only screen for managing the neighborhood hubs.
 *
 * Mirrors the web `/staff/hubs` page: list, create, rename, and delete (with
 * the `confirm: true` payload required by the backend safeguard).
 */
class HubManagementActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var txtStatus: TextView
    private lateinit var inputName: TextInputEditText
    private lateinit var inputSlug: TextInputEditText
    private lateinit var adapter: HubAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hub_management)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerHubs)
        txtStatus = findViewById(R.id.txtStatus)
        inputName = findViewById(R.id.inputHubName)
        inputSlug = findViewById(R.id.inputHubSlug)

        adapter = HubAdapter(
            onRename = { hub -> showRenameDialog(hub) },
            onDelete = { hub -> showDeleteDialog(hub) },
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener { createHub() }

        load()
    }

    private fun load() {
        showStatus(getString(R.string.staff_loading))
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@HubManagementActivity).listStaffHubs()
                if (res.isSuccessful) {
                    val hubs = res.body().orEmpty()
                    adapter.submit(hubs)
                    if (hubs.isEmpty()) showStatus(getString(R.string.staff_no_results))
                    else hideStatus()
                } else {
                    showStatus(getString(R.string.staff_request_failed))
                }
            } catch (_: Exception) {
                showStatus(getString(R.string.staff_request_failed))
            }
        }
    }

    private fun createHub() {
        val name = inputName.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            Toast.makeText(this, R.string.hub_name_hint, Toast.LENGTH_SHORT).show()
            return
        }
        val slug = inputSlug.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@HubManagementActivity)
                    .createStaffHub(HubCreateRequest(name = name, slug = slug))
                if (res.isSuccessful) {
                    Toast.makeText(
                        this@HubManagementActivity,
                        R.string.staff_action_succeeded,
                        Toast.LENGTH_SHORT,
                    ).show()
                    inputName.setText("")
                    inputSlug.setText("")
                    res.body()?.let { adapter.add(it) }
                } else {
                    Toast.makeText(
                        this@HubManagementActivity,
                        R.string.staff_request_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    this@HubManagementActivity,
                    R.string.staff_request_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun showRenameDialog(hub: Hub) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(hub.name)
            setSelection(hub.name.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.hub_action_rename)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                if (newName.isBlank() || newName == hub.name) return@setPositiveButton
                renameHub(hub, newName)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun renameHub(hub: Hub, newName: String) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@HubManagementActivity)
                    .updateStaffHub(hub.id, HubUpdateRequest(name = newName))
                if (res.isSuccessful) {
                    res.body()?.let { adapter.replace(it) }
                    Toast.makeText(
                        this@HubManagementActivity,
                        R.string.staff_action_succeeded,
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    Toast.makeText(
                        this@HubManagementActivity,
                        R.string.staff_request_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    this@HubManagementActivity,
                    R.string.staff_request_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun showDeleteDialog(hub: Hub) {
        AlertDialog.Builder(this)
            .setTitle(R.string.hub_delete_confirm_title)
            .setMessage(R.string.hub_delete_confirm_message)
            .setPositiveButton(R.string.hub_action_delete) { _, _ -> deleteHub(hub) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteHub(hub: Hub) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@HubManagementActivity)
                    .deleteStaffHub(hub.id, HubDeleteRequest(confirm = true))
                if (res.isSuccessful) {
                    adapter.remove(hub.id)
                    Toast.makeText(
                        this@HubManagementActivity,
                        R.string.staff_action_succeeded,
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    Toast.makeText(
                        this@HubManagementActivity,
                        R.string.staff_request_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    this@HubManagementActivity,
                    R.string.staff_request_failed,
                    Toast.LENGTH_SHORT,
                ).show()
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
}
