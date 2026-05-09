package com.bounswe2026group8.emergencyhub.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.ExpertiseDecisionRequest
import com.bounswe2026group8.emergencyhub.api.ExpertiseVerificationItem
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Verification-coordinator-or-admin screen: review expertise certification claims.
 *
 * REJECT requires a note (matches backend); APPROVE and reopen-to-PENDING accept
 * an optional note that simply gets stored on the audit log.
 */
class ExpertiseVerificationActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var txtStatus: TextView
    private lateinit var spinner: Spinner
    private lateinit var adapter: ExpertiseVerificationAdapter

    private val statusValues = arrayOf("PENDING", "APPROVED", "REJECTED", "ALL")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expertise_verification)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerVerifications)
        txtStatus = findViewById(R.id.txtStatus)
        spinner = findViewById(R.id.spinnerStatus)

        adapter = ExpertiseVerificationAdapter { item, decision -> decide(item, decision) }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val statusLabels = arrayOf(
            getString(R.string.staff_status_pending),
            getString(R.string.staff_status_approved),
            getString(R.string.staff_status_rejected),
            getString(R.string.staff_status_all),
        )
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusLabels,
        )
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = load()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<MaterialButton>(R.id.btnRefresh).setOnClickListener { load() }
        load()
    }

    private fun load() {
        val status = statusValues[spinner.selectedItemPosition]
        showStatus(getString(R.string.staff_loading))
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ExpertiseVerificationActivity)
                    .listExpertiseVerifications(status = status)
                if (res.isSuccessful) {
                    val items = res.body().orEmpty()
                    adapter.submit(items)
                    if (items.isEmpty()) showStatus(getString(R.string.staff_no_results))
                    else hideStatus()
                } else {
                    showStatus(getString(R.string.staff_request_failed))
                }
            } catch (_: Exception) {
                showStatus(getString(R.string.staff_request_failed))
            }
        }
    }

    private fun decide(item: ExpertiseVerificationItem, decision: String) {
        val rejecting = decision == "REJECTED"
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = if (rejecting) "Note (required)" else "Note (optional)"
        }
        AlertDialog.Builder(this)
            .setTitle(decision)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val note = input.text?.toString()?.trim().orEmpty()
                if (rejecting && note.isBlank()) {
                    Toast.makeText(this, R.string.staff_note_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                submit(item, decision, note)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun submit(item: ExpertiseVerificationItem, decision: String, note: String) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ExpertiseVerificationActivity)
                    .decideExpertiseVerification(
                        expertiseId = item.id,
                        body = ExpertiseDecisionRequest(
                            status = decision,
                            note = note.ifBlank { null },
                        ),
                    )
                if (res.isSuccessful) {
                    Toast.makeText(
                        this@ExpertiseVerificationActivity,
                        R.string.staff_action_succeeded,
                        Toast.LENGTH_SHORT,
                    ).show()
                    load()
                } else {
                    Toast.makeText(
                        this@ExpertiseVerificationActivity,
                        R.string.staff_request_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    this@ExpertiseVerificationActivity,
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
