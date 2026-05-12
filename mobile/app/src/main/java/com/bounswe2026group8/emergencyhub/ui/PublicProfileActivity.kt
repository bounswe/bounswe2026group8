package com.bounswe2026group8.emergencyhub.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.ExpertiseFieldData
import com.bounswe2026group8.emergencyhub.api.HelpOfferItem
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.ResourceData
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.UserBadgeItem
import com.bounswe2026group8.emergencyhub.api.UserPublicProfileData
import com.bounswe2026group8.emergencyhub.util.LocaleManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class PublicProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "user_id"

        /** Navigates to the correct profile screen: own profile if targetUserId == currentUserId, otherwise public profile. */
        fun navigate(context: Context, targetUserId: Int, currentUserId: Int?) {
            if (targetUserId == currentUserId) {
                context.startActivity(Intent(context, ProfileActivity::class.java))
            } else {
                context.startActivity(
                    Intent(context, PublicProfileActivity::class.java)
                        .putExtra(EXTRA_USER_ID, targetUserId)
                )
            }
        }
    }

    private var userId: Int = -1

    private lateinit var progressBar: ProgressBar
    private lateinit var contentContainer: LinearLayout
    
    private lateinit var txtAvatar: TextView
    private lateinit var txtFullName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtRoleBadge: TextView
    private lateinit var txtStatusBadge: TextView
    private lateinit var badgesRow: LinearLayout

    private lateinit var cardPersonalInfo: View
    private lateinit var personalInfoContainer: LinearLayout
    private lateinit var cardResources: View
    private lateinit var resourceList: LinearLayout
    private lateinit var cardExpertise: View
    private lateinit var expertiseList: LinearLayout

    // Activity section
    private enum class ActivityTab { POSTS, REQUESTS, OFFERS }
    private var activityTab = ActivityTab.POSTS
    private var userPosts: List<Post>? = null
    private var userRequests: List<HelpRequestItem>? = null
    private var userOffers: List<HelpOfferItem>? = null

    private lateinit var cardActivity: View
    private lateinit var activityTabPosts: TextView
    private lateinit var activityTabRequests: TextView
    private lateinit var activityTabOffers: TextView
    private lateinit var txtActivityState: TextView
    private lateinit var activityList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_public_profile)

        userId = intent.getIntExtra(EXTRA_USER_ID, -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        progressBar = findViewById(R.id.progressBar)
        contentContainer = findViewById(R.id.contentContainer)
        
        txtAvatar = findViewById(R.id.txtAvatar)
        txtFullName = findViewById(R.id.txtFullName)
        txtEmail = findViewById(R.id.txtEmail)
        txtRoleBadge = findViewById(R.id.txtRoleBadge)
        txtStatusBadge = findViewById(R.id.txtStatusBadge)
        badgesRow = findViewById(R.id.badgesRow)

        cardPersonalInfo = findViewById(R.id.cardPersonalInfo)
        personalInfoContainer = findViewById(R.id.personalInfoContainer)
        cardResources = findViewById(R.id.cardResources)
        resourceList = findViewById(R.id.resourceList)
        cardExpertise = findViewById(R.id.cardExpertise)
        expertiseList = findViewById(R.id.expertiseList)

        cardActivity = findViewById(R.id.cardActivity)
        activityTabPosts = findViewById(R.id.activityTabPosts)
        activityTabRequests = findViewById(R.id.activityTabRequests)
        activityTabOffers = findViewById(R.id.activityTabOffers)
        txtActivityState = findViewById(R.id.txtActivityState)
        activityList = findViewById(R.id.activityList)

        activityTabPosts.setOnClickListener { selectActivityTab(ActivityTab.POSTS) }
        activityTabRequests.setOnClickListener { selectActivityTab(ActivityTab.REQUESTS) }
        activityTabOffers.setOnClickListener { selectActivityTab(ActivityTab.OFFERS) }

        loadPublicProfile()
        loadUserBadges()
        initActivitySection()
    }

    private fun loadPublicProfile() {
        progressBar.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@PublicProfileActivity).getUserPublicProfile(userId)
                if (res.isSuccessful && res.body() != null) {
                    displayProfile(res.body()!!)
                } else {
                    Toast.makeText(this@PublicProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PublicProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                progressBar.visibility = View.GONE
                contentContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun loadUserBadges() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@PublicProfileActivity).getUserBadges(userId)
                if (res.isSuccessful) {
                    val badges = res.body() ?: emptyList()
                    val topBadges = badges
                        .filter { it.currentLevel > 0 }
                        .sortedByDescending { it.currentLevel }
                        .take(3)
                    displayTopBadges(topBadges)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun displayProfile(data: UserPublicProfileData) {
        txtAvatar.text = data.fullName.firstOrNull()?.uppercase() ?: "?"
        txtFullName.text = data.fullName
        txtEmail.text = data.email
        txtRoleBadge.text = if (data.role == "EXPERT") getString(R.string.role_expert) else getString(R.string.role_standard)

        val p = data.profile
        if (p != null) {
            val hasPersonalInfo = p.phoneNumber != null || p.bloodType != null ||
                    p.emergencyContact != null || p.emergencyContactPhone != null ||
                    p.specialNeeds != null || p.bio != null || p.preferredLanguage != null

            if (hasPersonalInfo) {
                cardPersonalInfo.visibility = View.VISIBLE
                
                // Keep the title, remove existing fields if reloading
                val childCount = personalInfoContainer.childCount
                if (childCount > 1) {
                    personalInfoContainer.removeViews(1, childCount - 1)
                }

                addField(getString(R.string.profile_phone), p.phoneNumber)
                addField(getString(R.string.profile_blood_type), p.bloodType)
                addField(getString(R.string.profile_emergency_contact), p.emergencyContact)
                addField(getString(R.string.profile_emergency_phone), p.emergencyContactPhone)
                addField(getString(R.string.profile_special_needs_label), p.specialNeeds)
                addField(getString(R.string.profile_bio), p.bio)
            }

            if (p.availabilityStatus.isNotBlank()) {
                txtStatusBadge.visibility = View.VISIBLE
                val (label, colorRes) = when (p.availabilityStatus) {
                    "NEEDS_HELP" -> getString(R.string.status_needs_help) to R.color.error
                    "AVAILABLE_TO_HELP" -> getString(R.string.status_available) to R.color.accent
                    else -> getString(R.string.status_safe) to R.color.success
                }
                txtStatusBadge.text = getString(R.string.profile_status_badge_format, label)
                txtStatusBadge.setTextColor(ContextCompat.getColor(this, colorRes))
            } else {
                txtStatusBadge.visibility = View.GONE
            }
        }

        val resList = data.resources
        if (!resList.isNullOrEmpty()) {
            cardResources.visibility = View.VISIBLE
            resourceList.removeAllViews()
            displayResources(resList)
        } else {
            cardResources.visibility = View.GONE
        }

        val expList = data.expertiseFields
        if (!expList.isNullOrEmpty() && data.role == "EXPERT") {
            cardExpertise.visibility = View.VISIBLE
            expertiseList.removeAllViews()
            displayExpertise(expList)
        } else {
            cardExpertise.visibility = View.GONE
        }
    }

    private fun addField(label: String, value: String?) {
        if (value.isNullOrBlank()) return

        val view = LayoutInflater.from(this).inflate(R.layout.include_profile_field, personalInfoContainer, false)
        view.findViewById<TextView>(R.id.fieldLabel).text = label
        view.findViewById<TextView>(R.id.fieldValue).text = value
        
        // include_profile_field.xml has no editIcon — layout is read-only by design
        // val icon = view.findViewById<View>(R.id.editIcon)
        // if (icon != null) { icon.visibility = View.GONE }
        view.isClickable = false
        
        personalInfoContainer.addView(view)
    }

    private fun displayResources(list: List<ResourceData>) {
        for (item in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_input))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            val label = TextView(this).apply {
                text = getString(R.string.profile_resource_item_format, item.name, item.category, item.quantity)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val condBadge = TextView(this).apply {
                text = if (item.condition) "\u2713" else "\u2717"
                setTextColor(ContextCompat.getColor(context, if (item.condition) R.color.success else R.color.error))
                textSize = 16f
                setPadding(16, 0, 16, 0)
            }

            row.addView(label)
            row.addView(condBadge)
            resourceList.addView(row)
        }
    }

    private fun displayExpertise(list: List<ExpertiseFieldData>) {
        for (item in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_input))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            val level = if (item.certificationLevel == "ADVANCED") {
                getString(R.string.profile_cert_level_advanced)
            } else {
                getString(R.string.profile_cert_level_beginner)
            }
            val label = TextView(this).apply {
                text = getString(R.string.profile_expertise_item_format, item.category.displayName(LocaleManager.getLanguage(context)), level)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            row.addView(label)
            expertiseList.addView(row)
        }
    }

    // ── Activity section ────────────────────────────────────────────────────

    private fun initActivitySection() {
        cardActivity.visibility = View.VISIBLE
        selectActivityTab(ActivityTab.POSTS)
    }

    private fun selectActivityTab(tab: ActivityTab) {
        activityTab = tab
        updateActivityTabStyles()
        when (tab) {
            ActivityTab.POSTS -> if (userPosts != null) displayActivityPosts() else loadActivityPosts()
            ActivityTab.REQUESTS -> if (userRequests != null) displayActivityRequests() else loadActivityRequests()
            ActivityTab.OFFERS -> if (userOffers != null) displayActivityOffers() else loadActivityOffers()
        }
    }

    private fun updateActivityTabStyles() {
        val allTabs = listOf(
            ActivityTab.POSTS to activityTabPosts,
            ActivityTab.REQUESTS to activityTabRequests,
            ActivityTab.OFFERS to activityTabOffers,
        )
        for ((t, view) in allTabs) {
            if (t == activityTab) {
                view.setTextColor(ContextCompat.getColor(this, R.color.accent))
                view.setBackgroundResource(R.drawable.sort_pill_active_bg)
            } else {
                view.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                view.setBackgroundResource(R.drawable.sort_pill_bg)
            }
        }
    }

    private fun loadActivityPosts() {
        showActivityLoading()
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@PublicProfileActivity).getPosts(author = userId)
                if (res.isSuccessful) {
                    userPosts = (res.body() ?: emptyList()).sortedByDescending { it.createdAt }
                    if (activityTab == ActivityTab.POSTS) displayActivityPosts()
                } else {
                    if (activityTab == ActivityTab.POSTS) showActivityError()
                }
            } catch (_: Exception) {
                if (activityTab == ActivityTab.POSTS) showActivityError()
            }
        }
    }

    private fun loadActivityRequests() {
        showActivityLoading()
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@PublicProfileActivity).getHelpRequests(author = userId)
                if (res.isSuccessful) {
                    userRequests = (res.body() ?: emptyList()).sortedByDescending { it.createdAt }
                    if (activityTab == ActivityTab.REQUESTS) displayActivityRequests()
                } else {
                    if (activityTab == ActivityTab.REQUESTS) showActivityError()
                }
            } catch (_: Exception) {
                if (activityTab == ActivityTab.REQUESTS) showActivityError()
            }
        }
    }

    private fun loadActivityOffers() {
        showActivityLoading()
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@PublicProfileActivity).getHelpOffers(author = userId)
                if (res.isSuccessful) {
                    userOffers = (res.body() ?: emptyList()).sortedByDescending { it.createdAt }
                    if (activityTab == ActivityTab.OFFERS) displayActivityOffers()
                } else {
                    if (activityTab == ActivityTab.OFFERS) showActivityError()
                }
            } catch (_: Exception) {
                if (activityTab == ActivityTab.OFFERS) showActivityError()
            }
        }
    }

    private fun displayActivityPosts() {
        val posts = userPosts ?: return
        activityList.removeAllViews()
        if (posts.isEmpty()) {
            showActivityEmpty(getString(R.string.profile_activity_empty_posts))
            return
        }
        txtActivityState.visibility = View.GONE
        for (post in posts) {
            val row = buildActivityRow(
                title = post.title,
                meta = getString(R.string.profile_activity_post_meta, post.upvoteCount, post.downvoteCount, post.commentCount)
            ) {
                val intent = Intent(this, PostDetailActivity::class.java)
                intent.putExtra("post_id", post.id)
                startActivity(intent)
            }
            activityList.addView(row)
        }
    }

    private fun displayActivityRequests() {
        val requests = userRequests ?: return
        activityList.removeAllViews()
        if (requests.isEmpty()) {
            showActivityEmpty(getString(R.string.profile_activity_empty_requests))
            return
        }
        txtActivityState.visibility = View.GONE
        for (item in requests) {
            val statusLabel = item.status.replace('_', ' ')
            val row = buildActivityRow(
                title = item.title,
                meta = getString(R.string.profile_activity_request_meta, item.category, statusLabel)
            ) {
                val intent = Intent(this, HelpRequestDetailActivity::class.java)
                intent.putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, item.id)
                startActivity(intent)
            }
            activityList.addView(row)
        }
    }

    private fun displayActivityOffers() {
        val offers = userOffers ?: return
        activityList.removeAllViews()
        if (offers.isEmpty()) {
            showActivityEmpty(getString(R.string.profile_activity_empty_offers))
            return
        }
        txtActivityState.visibility = View.GONE
        for (item in offers) {
            val row = buildActivityRow(
                title = item.skillOrResource,
                meta = getString(R.string.profile_activity_offer_meta, item.category, item.availability)
            ) { /* offers are read-only; tap does nothing */ }
            activityList.addView(row)
        }
    }

    private fun buildActivityRow(title: String, meta: String, onClick: () -> Unit): View {
        val density = resources.displayMetrics.density

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (10 * density).toInt(), 0, (10 * density).toInt())
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 2 }
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }

        val metaView = TextView(this).apply {
            text = meta
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (2 * density).toInt() }
        }

        row.addView(titleView)
        row.addView(metaView)
        return row
    }

    private fun showActivityLoading() {
        activityList.removeAllViews()
        txtActivityState.text = getString(R.string.profile_activity_loading)
        txtActivityState.visibility = View.VISIBLE
    }

    private fun showActivityEmpty(message: String) {
        activityList.removeAllViews()
        txtActivityState.text = message
        txtActivityState.visibility = View.VISIBLE
    }

    private fun showActivityError() {
        activityList.removeAllViews()
        txtActivityState.text = getString(R.string.network_error)
        txtActivityState.visibility = View.VISIBLE
    }

    private fun displayTopBadges(badges: List<UserBadgeItem>) {
        badgesRow.removeAllViews()
        if (badges.isEmpty()) {
            badgesRow.visibility = View.GONE
            return
        }
        badgesRow.visibility = View.VISIBLE

        val density = resources.displayMetrics.density

        for (badge in badges) {
            val localizedName = BadgeLocalizer.getLocalizedBadgeName(this, badge.badgeName)
            val displayTitle = if (badge.currentLevel > 0) {
                getString(R.string.badges_name_with_level, localizedName, badge.currentLevel)
            } else {
                localizedName
            }

            val pillText = getString(R.string.badges_pill_format, badge.badgeIcon, displayTitle)

            val pill = TextView(this).apply {
                text = pillText
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.accent))
                setBackgroundColor(ContextCompat.getColor(context, R.color.badge_accent_bg))
                setPadding(
                    (10 * density).toInt(), (4 * density).toInt(),
                    (10 * density).toInt(), (4 * density).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * density).toInt() }
            }

            badgesRow.addView(pill)
        }
    }
}
