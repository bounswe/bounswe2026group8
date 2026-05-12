package com.bounswe2026group8.emergencyhub.ui

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

        loadPublicProfile()
        loadUserBadges()
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
        
        // Remove click listeners and edit icons (they are read-only)
        val icon = view.findViewById<View>(R.id.editIcon)
        if (icon != null) {
            icon.visibility = View.GONE
        }
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
