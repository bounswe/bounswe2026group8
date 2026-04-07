package com.bounswe2026group8.emergencyhub.map.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.offline.ui.EmergencyContactsActivity
import com.bounswe2026group8.emergencyhub.offline.ui.FirstAidActivity
import com.google.android.material.button.MaterialButton

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

        // Back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

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

        // First aid page
        checklistButton.setOnClickListener {
            startActivity(Intent(
                this,
                FirstAidActivity::class.java))
        }

        // Contacts page
        contactsButton.setOnClickListener {
            startActivity(Intent(
                this,
                EmergencyContactsActivity::class.java))
        }
    }
}