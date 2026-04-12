package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.viewmodel.HelpCenterViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Displays the list of help offers with category filtering.
 *
 * Data is fetched from GET /help-offers/ and filtered client-side
 * via category chips. Tapping a card shows a detail dialog.
 * The logged-in user can delete their own offers.
 */
class HelpOfferListActivity : AppCompatActivity() {

    private val viewModel: HelpCenterViewModel by viewModels()
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

        observeViewModel()
        fetchHelpOffers()
    }

    private fun observeViewModel() {
        viewModel.offers.observe(this) { offers ->
            allItems = offers
            applyFilter()
        }
        viewModel.isLoading.observe(this) { loading ->
            if (loading) {
                progressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyState.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
            }
        }
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                showEmpty()
                viewModel.clearError()
            }
        }
        viewModel.navigateToLanding.observe(this) { if (it) navigateToLanding() }
        viewModel.offerDeletedMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, getString(R.string.help_offer_deleted), Toast.LENGTH_SHORT).show()
                if (adapter.itemCount == 0) showEmpty()
                viewModel.clearOfferDeletedMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            fetchHelpOffers()
        }
    }

    private fun fetchHelpOffers() {
        viewModel.fetchHelpOffers(null)
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

    private fun deleteOffer(item: HelpOfferItem) {
        viewModel.deleteOffer(item.id)
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
