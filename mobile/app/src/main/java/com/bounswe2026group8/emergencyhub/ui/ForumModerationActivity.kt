package com.bounswe2026group8.emergencyhub.ui

import android.app.AlertDialog
import android.content.Intent
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
import com.bounswe2026group8.emergencyhub.api.ForumModerationActionRequest
import com.bounswe2026group8.emergencyhub.api.ForumModerationPost
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Mod-or-admin screen: triage reported / hidden / removed posts.
 *
 * The status filter offers an "any" option (matches the backend default which
 * returns reported-or-non-active items). HIDE and REMOVE require a reason;
 * RESTORE does not.
 */
class ForumModerationActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var txtStatus: TextView
    private lateinit var spinner: Spinner
    private lateinit var adapter: ForumModerationAdapter

    private val statusValues = arrayOf("", "ACTIVE", "HIDDEN", "REMOVED")
    private val statusLabels by lazy {
        arrayOf(
            getString(R.string.staff_status_reported_or_nonactive),
            getString(R.string.staff_status_active),
            getString(R.string.staff_status_hidden),
            getString(R.string.staff_status_removed),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forum_moderation)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerPosts)
        txtStatus = findViewById(R.id.txtStatus)
        spinner = findViewById(R.id.spinnerStatus)

        adapter = ForumModerationAdapter(
            { post: ForumModerationPost -> openPost(post) },
            { post: ForumModerationPost, action: String -> moderate(post, action) },
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusLabels,
        )
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = loadPosts()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<MaterialButton>(R.id.btnRefresh).setOnClickListener { loadPosts() }
        loadPosts()
    }

    private fun loadPosts() {
        val statusFilter = statusValues[spinner.selectedItemPosition].ifBlank { null }
        showStatus(getString(R.string.staff_loading))
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ForumModerationActivity)
                    .listForumModerationPosts(status = statusFilter)
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

    private fun moderate(post: ForumModerationPost, action: String) {
        val needsReason = action == "HIDE" || action == "REMOVE"
        if (needsReason) {
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                hint = "Reason"
            }
            AlertDialog.Builder(this)
                .setTitle(action)
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val reason = input.text?.toString()?.trim().orEmpty()
                    if (reason.isBlank()) {
                        Toast.makeText(this, R.string.staff_reason_required, Toast.LENGTH_SHORT).show()
                    } else {
                        submitModeration(post, action, reason)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            submitModeration(post, action, null)
        }
    }

    private fun openPost(post: ForumModerationPost) {
        startActivity(
            Intent(this, PostDetailActivity::class.java)
                .putExtra("post_id", post.id),
        )
    }

    private fun submitModeration(post: ForumModerationPost, action: String, reason: String?) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ForumModerationActivity).moderateForumPost(
                    postId = post.id,
                    body = ForumModerationActionRequest(action = action, reason = reason),
                )
                if (res.isSuccessful) {
                    Toast.makeText(this@ForumModerationActivity, R.string.staff_action_succeeded, Toast.LENGTH_SHORT).show()
                    loadPosts()
                } else {
                    Toast.makeText(this@ForumModerationActivity, R.string.staff_request_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ForumModerationActivity, R.string.staff_request_failed, Toast.LENGTH_SHORT).show()
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
