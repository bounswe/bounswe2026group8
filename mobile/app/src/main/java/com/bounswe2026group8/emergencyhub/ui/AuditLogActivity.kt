package com.bounswe2026group8.emergencyhub.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Admin-only audit-log viewer.
 *
 * Read-only mirror of the web `/staff/audit-logs` page. Filters use the same
 * server-side query params (`action`, `target_type`).
 */
class AuditLogActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var txtStatus: TextView
    private lateinit var inputAction: TextInputEditText
    private lateinit var inputTargetType: TextInputEditText
    private lateinit var adapter: AuditLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audit_log)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerLogs)
        txtStatus = findViewById(R.id.txtStatus)
        inputAction = findViewById(R.id.inputAction)
        inputTargetType = findViewById(R.id.inputTargetType)

        adapter = AuditLogAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<MaterialButton>(R.id.btnApply).setOnClickListener { load() }

        load()
    }

    private fun load() {
        val action = inputAction.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val targetType = inputTargetType.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        showStatus(getString(R.string.staff_loading))
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@AuditLogActivity)
                    .listAuditLogs(action = action, targetType = targetType)
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

    private fun showStatus(message: String) {
        txtStatus.text = message
        txtStatus.visibility = View.VISIBLE
    }

    private fun hideStatus() {
        txtStatus.visibility = View.GONE
    }
}
