package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Admin-only landing page that surfaces every functionality the admin role can
 * perform. Admins navigate here from the staff dashboard's "Admin Tools" card.
 *
 * Backend permission classes still enforce who can call which endpoint; this
 * screen is just a UI gateway for already-elevated users.
 */
class AdminToolsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_tools)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialCardView>(R.id.cardAdminUsers).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardAdminHubs).setOnClickListener {
            startActivity(Intent(this, HubManagementActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardForumModeration).setOnClickListener {
            startActivity(Intent(this, ForumModerationActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardHelpModeration).setOnClickListener {
            startActivity(Intent(this, HelpModerationActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardExpertiseVerification).setOnClickListener {
            startActivity(Intent(this, ExpertiseVerificationActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardAuditLog).setOnClickListener {
            startActivity(Intent(this, AuditLogActivity::class.java))
        }
    }
}
