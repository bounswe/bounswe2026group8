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
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Displays the list of help offers with category filtering.
 *
 * Data is fetched from GET /help-offers/ and filtered client-side
 * via category chips. Tapping a card shows a detail dialog.
 * The logged-in user can delete their own offers.
 */
class HelpOfferListActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: HelpOfferAdapter

    /** Full unfiltered list from the API. */
    private var allItems: List<HelpOfferItem> = emptyList()

    /** Currently selected category filter (null = "All"). */
    private var selectedCategory: String? = null

    /** ID of the currently logged-in user, used for showing delete buttons. */
    private var currentUserId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_offer_list)

        tokenManager = TokenManager(this)

        // Auth guard
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        // Retrieve cached user ID for ownership checks
        currentUserId = tokenManager.getUser()?.id

        // Bind views
        recyclerView = findViewById(R.id.recyclerHelpOffers)
        emptyState = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)
        chipGroup = findViewById(R.id.chipGroupCategory)

        // Back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        // Create button
        findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener {
            startActivity(Intent(this, CreateHelpOfferActivity::class.java))
        }

        // RecyclerView setup
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HelpOfferAdapter(
            items = mutableListOf(),
            currentUserId = currentUserId,
            onItemClick = { showDetailDialog(it) },
            onDeleteClick = { confirmDelete(it) }
        )
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

        fetchHelpOffers()
    }

    /** Re-fetch on resume so the list updates after creating a new offer. */
    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            fetchHelpOffers()
        }
    }

    /** Fetches all help offers from the backend. */
    private fun fetchHelpOffers() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpOfferListActivity)
                    .getHelpOffers()

                if (response.isSuccessful) {
                    allItems = response.body() ?: emptyList()
                    applyFilter()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    Toast.makeText(
                        this@HelpOfferListActivity,
                        "Failed to load help offers",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmpty()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpOfferListActivity,
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

    /** Shows a MaterialAlertDialog with full offer details. */
    private fun showDetailDialog(item: HelpOfferItem) {
        val category = item.category.lowercase().replaceFirstChar { it.uppercase() }
        val message = buildString {
            append("Category: $category\n")
            append("Availability: ${item.availability}\n\n")
            append(item.description)
            append("\n\nOffered by ${item.author.fullName}")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(item.skillOrResource)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    /** Shows a confirmation dialog, then deletes the offer on confirm. */
    private fun confirmDelete(item: HelpOfferItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_offer))
            .setMessage(getString(R.string.delete_offer_confirm))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteOffer(item) }
            .show()
    }

    /** Calls DELETE /help-offers/{id}/ and removes the item from the adapter. */
    private fun deleteOffer(item: HelpOfferItem) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpOfferListActivity)
                    .deleteHelpOffer(item.id)

                // Treat 204 No Content (and any 2xx) as success
                if (response.code() == 204 || response.isSuccessful) {
                    adapter.removeItem(item.id)
                    allItems = allItems.filter { it.id != item.id }
                    Toast.makeText(
                        this@HelpOfferListActivity,
                        getString(R.string.help_offer_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    if (adapter.itemCount == 0) showEmpty()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    Toast.makeText(
                        this@HelpOfferListActivity,
                        "Failed to delete offer",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                if (e.message?.contains("HTTP 204") == true) {
                    adapter.removeItem(item.id)
                    allItems = allItems.filter { it.id != item.id }
                    Toast.makeText(
                        this@HelpOfferListActivity,
                        getString(R.string.help_offer_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    if (adapter.itemCount == 0) showEmpty()
                } else {
                    Toast.makeText(
                        this@HelpOfferListActivity,
                        "Network error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
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
