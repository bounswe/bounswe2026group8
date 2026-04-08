package com.bounswe2026group8.emergencyhub.map.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.offline.ui.EmergencyContactsActivity
import com.bounswe2026group8.emergencyhub.offline.ui.FirstAidActivity

/**
 * Entry screen for offline features.
 *
 * Currently includes:
 * - Offline Map
 * - Emergency checklist (first aid)
 * - Offline emergency contacts
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

        checklistButton.setOnClickListener {
            startActivity(Intent(this, FirstAidActivity::class.java))
        }

        contactsButton.setOnClickListener {
            startActivity(Intent(this, EmergencyContactsActivity::class.java))
        }
    }
}