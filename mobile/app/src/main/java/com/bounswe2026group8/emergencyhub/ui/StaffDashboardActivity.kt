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
 * Mirrors the web `/staff` dashboard: shows only the cards the current user's
 * staff role grants access to, with an obvious back button to the regular
 * dashboard.
 */
class StaffDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_dashboard)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        val user = TokenManager(this).getUser()
        val staffRole = user?.staffRole ?: "NONE"

        val txtRole = findViewById<TextView>(R.id.txtStaffRole)
        txtRole.text = StaffRoleHelper.label(staffRole)

        val cardAdminUsers = findViewById<MaterialCardView>(R.id.cardAdminUsers)
        val cardForumModeration = findViewById<MaterialCardView>(R.id.cardForumModeration)
        val cardExpertiseVerification = findViewById<MaterialCardView>(R.id.cardExpertiseVerification)
        val txtNoRoleHint = findViewById<TextView>(R.id.txtNoRoleHint)

        if (StaffRoleHelper.isAdmin(staffRole)) {
            cardAdminUsers.visibility = View.VISIBLE
        }
        if (StaffRoleHelper.canModerate(staffRole)) {
            cardForumModeration.visibility = View.VISIBLE
        }
        if (StaffRoleHelper.canVerifyExpertise(staffRole)) {
            cardExpertiseVerification.visibility = View.VISIBLE
        }

        if (cardAdminUsers.visibility != View.VISIBLE &&
            cardForumModeration.visibility != View.VISIBLE &&
            cardExpertiseVerification.visibility != View.VISIBLE
        ) {
            txtNoRoleHint.visibility = View.VISIBLE
        }

        cardAdminUsers.setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        cardForumModeration.setOnClickListener {
            startActivity(Intent(this, ForumModerationActivity::class.java))
        }
        cardExpertiseVerification.setOnClickListener {
            startActivity(Intent(this, ExpertiseVerificationActivity::class.java))
        }
    }
}
