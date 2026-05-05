package com.bounswe2026group8.emergencyhub.ui

import android.app.AlertDialog
import android.content.Intent
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
import com.bounswe2026group8.emergencyhub.api.HelpOfferModerationItem
import com.bounswe2026group8.emergencyhub.api.HelpRequestModerationItem
import com.bounswe2026group8.emergencyhub.api.ModerationDeleteRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Mod-or-admin screen: triage help requests and offers.
 *
 * Backend's DELETE endpoints accept an optional `reason` body that's recorded
 * to the audit log. We always send a reason for clarity, requiring it on the
 * UI side so the audit trail is always populated.
 */
class HelpModerationActivity : AppCompatActivity() {

    private enum class Tab { REQUESTS, OFFERS }

    private lateinit var recycler: RecyclerView
    private lateinit var txtStatus: TextView
    private lateinit var btnTabRequests: MaterialButton
    private lateinit var btnTabOffers: MaterialButton
    private val adapter = HelpModerationAdapter(
        { item: HelpRequestModerationItem -> openHelpRequest(item) },
        { item: Any -> showDeleteDialog(item) },
    )
    private var currentTab = Tab.REQUESTS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_moderation)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerHelp)
        txtStatus = findViewById(R.id.txtStatus)
        btnTabRequests = findViewById(R.id.btnTabRequests)
        btnTabOffers = findViewById(R.id.btnTabOffers)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnTabRequests.setOnClickListener { switchTab(Tab.REQUESTS) }
        btnTabOffers.setOnClickListener { switchTab(Tab.OFFERS) }

        switchTab(Tab.REQUESTS)
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        load()
    }

    private fun load() {
        showStatus(getString(R.string.staff_loading))
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.getService(this@HelpModerationActivity)
                if (currentTab == Tab.REQUESTS) {
                    val res = service.listHelpRequestModeration()
                    if (res.isSuccessful) {
                        val items = res.body().orEmpty()
                        adapter.submitRequests(items)
                        if (items.isEmpty()) showStatus(getString(R.string.staff_no_results))
                        else hideStatus()
                    } else {
                        showStatus(getString(R.string.staff_request_failed))
                    }
                } else {
                    val res = service.listHelpOfferModeration()
                    if (res.isSuccessful) {
                        val items = res.body().orEmpty()
                        adapter.submitOffers(items)
                        if (items.isEmpty()) showStatus(getString(R.string.staff_no_results))
                        else hideStatus()
                    } else {
                        showStatus(getString(R.string.staff_request_failed))
                    }
                }
            } catch (_: Exception) {
                showStatus(getString(R.string.staff_request_failed))
            }
        }
    }

    private fun showDeleteDialog(item: Any) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = "Reason"
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.hub_action_delete)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val reason = input.text?.toString()?.trim().orEmpty()
                if (reason.isBlank()) {
                    Toast.makeText(this, R.string.staff_reason_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                deleteItem(item, reason)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openHelpRequest(item: HelpRequestModerationItem) {
        startActivity(
            Intent(this, HelpRequestDetailActivity::class.java)
                .putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, item.id),
        )
    }

    private fun deleteItem(item: Any, reason: String) {
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.getService(this@HelpModerationActivity)
                val res = when (item) {
                    is HelpRequestModerationItem ->
                        service.moderationDeleteHelpRequest(item.id, ModerationDeleteRequest(reason))
                    is HelpOfferModerationItem ->
                        service.moderationDeleteHelpOffer(item.id, ModerationDeleteRequest(reason))
                    else -> return@launch
                }
                if (res.isSuccessful) {
                    Toast.makeText(
                        this@HelpModerationActivity,
                        R.string.staff_action_succeeded,
                        Toast.LENGTH_SHORT,
                    ).show()
                    load()
                } else {
                    Toast.makeText(
                        this@HelpModerationActivity,
                        R.string.staff_request_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    this@HelpModerationActivity,
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
