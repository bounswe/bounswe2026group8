package com.bounswe2026group8.emergencyhub.offline.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R

/**
 * Entry screen for offline features.
 *
 * Currently includes:
 * - Offline Map (implemented)
 * - Checklist (placeholder)
 * - Contacts (placeholder)
 */
class OfflineFeaturesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load layout
        setContentView(R.layout.activity_offline_features)

        // Buttons
        val mapButton = findViewById<Button>(R.id.btnOpenMap)
        val checklistButton = findViewById<Button>(R.id.btnChecklist)
        val contactsButton = findViewById<Button>(R.id.btnContacts)

        /**
         * Open offline map screen
         */
        mapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        /**
         * Placeholder: checklist feature not implemented yet
         */
        checklistButton.setOnClickListener {
            startActivity(Intent(
                this,
                FirstAidActivity::class.java))
        }

        /**
         * Placeholder: contacts feature not implemented yet
         */
        contactsButton.setOnClickListener {
            startActivity(Intent(
                this,
                EmergencyContactsActivity::class.java))
        }
    }
}