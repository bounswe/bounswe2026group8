package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/**
 * Displays the list of active help requests with category filtering.
 *
 * Data is fetched once from GET /help-requests/ and filtered client-side
 * when the user taps a category chip. Tapping a list item will navigate
 * to the detail screen (placeholder Toast for now).
 */
class HelpRequestListActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: HelpRequestAdapter

    /** Full unfiltered list from the API. */
    private var allItems: List<HelpRequestItem> = emptyList()

    /** Currently selected category filter (null = "All"). */
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_request_list)

        tokenManager = TokenManager(this)

        // Auth guard — redirect if not logged in
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        // Bind views
        recyclerView = findViewById(R.id.recyclerHelpRequests)
        emptyState = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)
        chipGroup = findViewById(R.id.chipGroupCategory)

        // Back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        // Create button
        findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener {
            startActivity(Intent(this, CreateHelpRequestActivity::class.java))
        }

        // RecyclerView setup
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HelpRequestAdapter(emptyList()) { item -> onItemClick(item) }
        recyclerView.adapter = adapter

        // Category filter chips
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when {
                checkedIds.isEmpty() -> null
                checkedIds[0] == R.id.chipAll -> null
                checkedIds[0] == R.id.chipMedical -> "MEDICAL"
                checkedIds[0] == R.id.chipFood -> "FOOD"
                checkedIds[0] == R.id.chipShelter -> "SHELTER"
                checkedIds[0] == R.id.chipTransport -> "TRANSPORT"
                else -> null
            }
            applyFilter()
        }

        fetchHelpRequests()
    }

    /** Re-fetch on resume so the list updates after creating a new request. */
    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            fetchHelpRequests()
        }
    }

    /** Fetches all help requests from the backend. */
    private fun fetchHelpRequests() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestListActivity)
                    .getHelpRequests()

                if (response.isSuccessful) {
                    allItems = response.body() ?: emptyList()
                    applyFilter()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    Toast.makeText(
                        this@HelpRequestListActivity,
                        "Failed to load help requests",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmpty()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestListActivity,
                    "Network error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                showEmpty()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /** Filters [allItems] by [selectedCategory] and updates the UI. */
    private fun applyFilter() {
        val filtered = if (selectedCategory == null) {
            allItems
        } else {
            allItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        adapter.updateItems(filtered)

        if (filtered.isEmpty()) {
            showEmpty()
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun onItemClick(item: HelpRequestItem) {
        val intent = Intent(this, HelpRequestDetailActivity::class.java)
        intent.putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, item.id)
        startActivity(intent)
    }

    private fun showEmpty() {
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun navigateToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
