package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Mobile entry point for the staff toolset.
 *
 * Role-aware navigation hub:
 *   - Admin       → single "Admin Tools" card opens [AdminToolsActivity],
 *                    which surfaces every admin functionality (user/hub mgmt,
 *                    moderation, verification, audit log) in one place.
 *   - Moderator   → "Forum Moderation" + "Help Moderation" cards.
 *   - Verifier    → "Expertise Verification" card.
 *
 * Backend authorization is the source of truth; these helpers only gate the UI.
 */
class StaffDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_dashboard)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        val user = TokenManager(this).getUser()
        val staffRole = user?.staffRole ?: StaffRoleHelper.NONE

        findViewById<TextView>(R.id.txtStaffRole).text = StaffRoleHelper.label(staffRole)

        val cardAdminTools = findViewById<MaterialCardView>(R.id.cardAdminTools)
        val cardForumModeration = findViewById<MaterialCardView>(R.id.cardForumModeration)
        val cardHelpModeration = findViewById<MaterialCardView>(R.id.cardHelpModeration)
        val cardExpertiseVerification = findViewById<MaterialCardView>(R.id.cardExpertiseVerification)
        val txtNoRoleHint = findViewById<TextView>(R.id.txtNoRoleHint)

        if (StaffRoleHelper.isAdmin(staffRole)) {
            cardAdminTools.visibility = View.VISIBLE
        } else {
            if (StaffRoleHelper.canModerate(staffRole)) {
                cardForumModeration.visibility = View.VISIBLE
                cardHelpModeration.visibility = View.VISIBLE
            }
            if (StaffRoleHelper.canVerifyExpertise(staffRole)) {
                cardExpertiseVerification.visibility = View.VISIBLE
            }
        }

        if (cardAdminTools.visibility != View.VISIBLE &&
            cardForumModeration.visibility != View.VISIBLE &&
            cardHelpModeration.visibility != View.VISIBLE &&
            cardExpertiseVerification.visibility != View.VISIBLE
        ) {
            txtNoRoleHint.visibility = View.VISIBLE
        }

        cardAdminTools.setOnClickListener {
            startActivity(Intent(this, AdminToolsActivity::class.java))
        }
        cardForumModeration.setOnClickListener {
            startActivity(Intent(this, ForumModerationActivity::class.java))
        }
        cardHelpModeration.setOnClickListener {
            startActivity(Intent(this, HelpModerationActivity::class.java))
        }
        cardExpertiseVerification.setOnClickListener {
            startActivity(Intent(this, ExpertiseVerificationActivity::class.java))
        }
    }
}
