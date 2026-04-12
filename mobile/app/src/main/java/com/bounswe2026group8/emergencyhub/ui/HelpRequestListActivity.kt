package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Hub
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.viewmodel.HelpCenterViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Tabbed Help Center screen with "Requests" and "Offers" tabs.
 *
 * Both tabs share the same category filter chips. Switching tabs
 * fetches the relevant data and swaps the visible RecyclerView.
 * Matches the frontend's HelpRequestsPage tabbed layout.
 */
class HelpRequestListActivity : AppCompatActivity() {

    private val viewModel: HelpCenterViewModel by viewModels()
    private lateinit var tokenManager: TokenManager
    private lateinit var hubSelectorHelper: HubSelectorHelper
    private var selectedHub: Hub? = null

    // Views
    private lateinit var recyclerRequests: RecyclerView
    private lateinit var recyclerOffers: RecyclerView
    private lateinit var emptyState: View
    private lateinit var txtEmptyIcon: TextView
    private lateinit var txtEmptyTitle: TextView
    private lateinit var txtEmptyHint: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroup: ChipGroup
    private lateinit var tabRequests: TextView
    private lateinit var tabOffers: TextView
    private lateinit var btnCreate: MaterialButton

    // Adapters
    private lateinit var requestAdapter: HelpRequestAdapter
    private lateinit var offerAdapter: HelpOfferAdapter

    /** "requests" or "offers" */
    private var activeTab = "requests"

    /** Currently selected category filter (null = "All"). */
    private var selectedCategory: String? = null

    /** Full unfiltered lists from the API. */
    private var allRequests: List<HelpRequestItem> = emptyList()
    private var allOffers: List<HelpOfferItem> = emptyList()

    /** ID of the currently logged-in user, for showing delete on own offers. */
    private var currentUserId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_request_list)

        tokenManager = TokenManager(this)

        // Auth guard
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        currentUserId = tokenManager.getUser()?.id

        // Bind views
        recyclerRequests = findViewById(R.id.recyclerHelpRequests)
        recyclerOffers = findViewById(R.id.recyclerHelpOffers)
        emptyState = findViewById(R.id.emptyState)
        txtEmptyIcon = findViewById(R.id.txtEmptyIcon)
        txtEmptyTitle = findViewById(R.id.txtEmptyTitle)
        txtEmptyHint = findViewById(R.id.txtEmptyHint)
        progressBar = findViewById(R.id.progressBar)
        chipGroup = findViewById(R.id.chipGroupCategory)
        tabRequests = findViewById(R.id.tabRequests)
        tabOffers = findViewById(R.id.tabOffers)
        btnCreate = findViewById(R.id.btnCreate)

        // Back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        // Create button — action depends on active tab
        btnCreate.setOnClickListener {
            if (activeTab == "requests") {
                startActivity(Intent(this, CreateHelpRequestActivity::class.java))
            } else {
                startActivity(Intent(this, CreateHelpOfferActivity::class.java))
            }
        }

        // RecyclerView setup — Requests
        recyclerRequests.layoutManager = LinearLayoutManager(this)
        requestAdapter = HelpRequestAdapter(emptyList()) { item ->
            val intent = Intent(this, HelpRequestDetailActivity::class.java)
            intent.putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, item.id)
            startActivity(intent)
        }
        recyclerRequests.adapter = requestAdapter

        // RecyclerView setup — Offers
        recyclerOffers.layoutManager = LinearLayoutManager(this)
        offerAdapter = HelpOfferAdapter(
            items = mutableListOf(),
            currentUserId = currentUserId,
            onItemClick = { showOfferDetailDialog(it) },
            onDeleteClick = { confirmDeleteOffer(it) }
        )
        recyclerOffers.adapter = offerAdapter

        // Tab switching
        tabRequests.setOnClickListener { switchTab("requests") }
        tabOffers.setOnClickListener { switchTab("offers") }

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

        // Hub selector
        hubSelectorHelper = HubSelectorHelper(
            this,
            findViewById<Spinner>(R.id.spinnerHubSelector),
            onHubSelected = { hub ->
                selectedHub = hub
                if (activeTab == "requests") fetchHelpRequests() else fetchHelpOffers()
            }
        )
        hubSelectorHelper.load()

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.requests.observe(this) { requests ->
            allRequests = requests
            if (activeTab == "requests") applyFilter()
        }
        viewModel.offers.observe(this) { offers ->
            allOffers = offers
            if (activeTab == "offers") applyFilter()
        }
        viewModel.isLoading.observe(this) { loading ->
            if (loading) showLoading()
            else progressBar.visibility = View.GONE
        }
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                showEmpty()
                viewModel.clearError()
            }
        }
        viewModel.navigateToLanding.observe(this) { if (it) navigateToLanding() }
        viewModel.offerDeletedMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, getString(R.string.help_offer_deleted), Toast.LENGTH_SHORT).show()
                viewModel.clearOfferDeletedMessage()
            }
        }
    }

    /** Re-fetch on resume so lists update after creating or deleting an item. */
    override fun onResume() {
        super.onResume()
        if (::hubSelectorHelper.isInitialized) {
            hubSelectorHelper.load()
        }
        if (activeTab == "requests") fetchHelpRequests() else fetchHelpOffers()
    }

    // ── Tab switching ────────────────────────────────────────────────────

    private fun switchTab(tab: String) {
        if (activeTab == tab) return
        activeTab = tab

        // Update tab visual state
        if (tab == "requests") {
            tabRequests.setTextColor(getColor(R.color.accent))
            tabRequests.setBackgroundResource(R.drawable.tab_active_bg)
            tabOffers.setTextColor(getColor(R.color.text_muted))
            tabOffers.background = null
        } else {
            tabOffers.setTextColor(getColor(R.color.accent))
            tabOffers.setBackgroundResource(R.drawable.tab_active_bg)
            tabRequests.setTextColor(getColor(R.color.text_muted))
            tabRequests.background = null
        }

        // Reset category filter to "All"
        chipGroup.check(R.id.chipAll)
        selectedCategory = null

        // Fetch data for the newly active tab
        if (tab == "requests") {
            fetchHelpRequests()
        } else {
            fetchHelpOffers()
        }
    }

    // ── Data fetching ────────────────────────────────────────────────────

    private fun fetchHelpRequests() {
        viewModel.fetchHelpRequests(selectedHub?.id)
    }

    private fun fetchHelpOffers() {
        viewModel.fetchHelpOffers(selectedHub?.id)
    }

    // ── Filtering ────────────────────────────────────────────────────────

    /** Filters the active tab's data by [selectedCategory] and updates the UI. */
    private fun applyFilter() {
        if (activeTab == "requests") {
            val filtered = if (selectedCategory == null) allRequests
            else allRequests.filter { it.category.equals(selectedCategory, ignoreCase = true) }

            requestAdapter.updateItems(filtered)
            recyclerRequests.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
            recyclerOffers.visibility = View.GONE

            if (filtered.isEmpty()) showEmpty() else emptyState.visibility = View.GONE
        } else {
            val filtered = if (selectedCategory == null) allOffers
            else allOffers.filter { it.category.equals(selectedCategory, ignoreCase = true) }

            offerAdapter.updateItems(filtered)
            recyclerOffers.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
            recyclerRequests.visibility = View.GONE

            if (filtered.isEmpty()) showEmpty() else emptyState.visibility = View.GONE
        }
    }

    // ── Offer detail dialog ──────────────────────────────────────────────

    /** Shows a MaterialAlertDialog with full offer details. */
    private fun showOfferDetailDialog(item: HelpOfferItem) {
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

    // ── Offer deletion ───────────────────────────────────────────────────

    /** Shows a confirmation dialog, then deletes the offer on confirm. */
    private fun confirmDeleteOffer(item: HelpOfferItem) {
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

    // ── UI helpers ───────────────────────────────────────────────────────

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerRequests.visibility = View.GONE
        recyclerOffers.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun showEmpty() {
        recyclerRequests.visibility = View.GONE
        recyclerOffers.visibility = View.GONE
        emptyState.visibility = View.VISIBLE

        // Update empty state text/icon based on active tab
        if (activeTab == "requests") {
            txtEmptyIcon.text = "📋"
            txtEmptyTitle.text = getString(R.string.empty_help_requests)
            txtEmptyHint.text = getString(R.string.empty_help_requests_hint)
        } else {
            txtEmptyIcon.text = "🤝"
            txtEmptyTitle.text = getString(R.string.empty_help_offers)
            txtEmptyHint.text = getString(R.string.empty_help_offers_hint)
        }
    }

    private fun navigateToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
