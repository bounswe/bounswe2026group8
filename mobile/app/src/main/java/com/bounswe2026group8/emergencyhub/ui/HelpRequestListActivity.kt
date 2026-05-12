package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import com.bounswe2026group8.emergencyhub.api.Hub
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.util.BadgeUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class HelpRequestListActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var hubSelectorHelper: HubSelectorHelper
    private var selectedHub: Hub? = null

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

    private lateinit var requestAdapter: HelpRequestAdapter
    private lateinit var offerAdapter: HelpOfferAdapter

    private var activeTab = "requests"

    private var expertiseMatchEnabled: Boolean = true
    private var selectedCategory: String? = null
    private var allRequests: List<HelpRequestItem> = emptyList()
    private var allOffers: List<HelpOfferItem> = emptyList()
    private var currentUserId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_request_list)

        tokenManager = TokenManager(this)
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        currentUserId = tokenManager.getUser()?.id

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

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        btnCreate.setOnClickListener {
            if (activeTab == "requests") {
                startActivity(Intent(this, CreateHelpRequestActivity::class.java))
            } else {
                startActivity(Intent(this, CreateHelpOfferActivity::class.java))
            }
        }

        recyclerRequests.layoutManager = LinearLayoutManager(this)
        requestAdapter = HelpRequestAdapter(emptyList(), currentUserId) { item ->
            val intent = Intent(this, HelpRequestDetailActivity::class.java)
            intent.putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, item.id)
            startActivity(intent)
        }
        recyclerRequests.adapter = requestAdapter

        recyclerOffers.layoutManager = LinearLayoutManager(this)
        offerAdapter = HelpOfferAdapter(
            items = mutableListOf(),
            currentUserId = currentUserId,
            onItemClick = { showOfferDetailDialog(it) },
            onDeleteClick = { confirmDeleteOffer(it) }
        )
        recyclerOffers.adapter = offerAdapter

        tabRequests.setOnClickListener { switchTab("requests") }
        tabOffers.setOnClickListener { switchTab("offers") }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when {
                checkedIds.isEmpty() -> null
                checkedIds[0] == R.id.chipAll -> null
                checkedIds[0] == R.id.chipMedical -> "MEDICAL"
                checkedIds[0] == R.id.chipFood -> "FOOD"
                checkedIds[0] == R.id.chipShelter -> "SHELTER"
                checkedIds[0] == R.id.chipTransport -> "TRANSPORT"
                checkedIds[0] == R.id.chipOther -> "OTHER"
                else -> null
            }
            applyFilter()
        }

        hubSelectorHelper = HubSelectorHelper(
            this,
            findViewById<TextView>(R.id.textHubDisplay),
            onHubSelected = { hub ->
                selectedHub = hub
                if (activeTab == "requests") fetchHelpRequests() else fetchHelpOffers()
            }
        )
        hubSelectorHelper.load()

        // Expertise-match toggle — visible only for EXPERT users
        val isExpert = tokenManager.getUser()?.role == "EXPERT"
        if (isExpert) {
            findViewById<View>(R.id.layoutExpertiseToggle).visibility = View.VISIBLE
            findViewById<SwitchMaterial>(R.id.switchExpertiseMatch).setOnCheckedChangeListener { _, checked ->
                expertiseMatchEnabled = checked
                if (activeTab == "requests") fetchHelpRequests()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::hubSelectorHelper.isInitialized) {
            hubSelectorHelper.load()
        }
        if (activeTab == "requests") fetchHelpRequests() else fetchHelpOffers()
    }

    private fun switchTab(tab: String) {
        if (activeTab == tab) return
        activeTab = tab

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

        chipGroup.check(R.id.chipAll)
        selectedCategory = null

        if (tab == "requests") fetchHelpRequests() else fetchHelpOffers()
    }

    private fun fetchHelpRequests() {
        showLoading()
        lifecycleScope.launch {
            try {
                val isExpert = tokenManager.getUser()?.role == "EXPERT"
                val response = RetrofitClient.getService(this@HelpRequestListActivity)
                    .getHelpRequests(
                        hubId = selectedHub?.id,
                        expertiseMatch = if (isExpert && expertiseMatchEnabled) true else null,
                    )

                if (response.isSuccessful) {
                    allRequests = response.body() ?: emptyList()
                    applyFilter()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    Toast.makeText(
                        this@HelpRequestListActivity,
                        getString(R.string.help_requests_load_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmpty()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestListActivity,
                    getString(R.string.network_error_with_message, e.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
                showEmpty()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun fetchHelpOffers() {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestListActivity)
                    .getHelpOffers(hubId = selectedHub?.id)

                if (response.isSuccessful) {
                    allOffers = response.body() ?: emptyList()
                    applyFilter()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    Toast.makeText(
                        this@HelpRequestListActivity,
                        getString(R.string.help_offers_load_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmpty()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestListActivity,
                    getString(R.string.network_error_with_message, e.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
                showEmpty()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

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

    private fun showOfferDetailDialog(item: HelpOfferItem) {
        val message = buildString {
            append(getString(R.string.help_offer_detail_category_format, BadgeUtils.formatCategoryLabel(this@HelpRequestListActivity, item.category)))
            append('\n')
            append(getString(R.string.help_offer_detail_availability_format, BadgeUtils.formatAvailabilityLabel(this@HelpRequestListActivity, item.availability)))
            append("\n\n")
            append(item.description)
            append("\n\n")
            append(getString(R.string.help_offer_author_format, item.author.fullName))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(item.skillOrResource)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private fun confirmDeleteOffer(item: HelpOfferItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_offer))
            .setMessage(getString(R.string.delete_offer_confirm))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteOffer(item) }
            .show()
    }

    private fun deleteOffer(item: HelpOfferItem) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestListActivity)
                    .deleteHelpOffer(item.id)

                if (response.code() == 204 || response.isSuccessful) {
                    onOfferDeleted(item.id)
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    Toast.makeText(
                        this@HelpRequestListActivity,
                        getString(R.string.help_offer_delete_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                if (e.message?.contains("HTTP 204") == true) {
                    onOfferDeleted(item.id)
                } else {
                    Toast.makeText(
                        this@HelpRequestListActivity,
                        getString(R.string.network_error_with_message, e.localizedMessage ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun onOfferDeleted(offerId: Int) {
        offerAdapter.removeItem(offerId)
        allOffers = allOffers.filter { it.id != offerId }
        Toast.makeText(this, getString(R.string.help_offer_deleted), Toast.LENGTH_SHORT).show()
        if (offerAdapter.itemCount == 0) showEmpty()
    }

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

        if (activeTab == "requests") {
            txtEmptyIcon.text = getString(R.string.empty_icon_requests)
            txtEmptyTitle.text = getString(R.string.empty_help_requests)
            txtEmptyHint.text = getString(R.string.empty_help_requests_hint)
        } else {
            txtEmptyIcon.text = getString(R.string.empty_icon_offers)
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
