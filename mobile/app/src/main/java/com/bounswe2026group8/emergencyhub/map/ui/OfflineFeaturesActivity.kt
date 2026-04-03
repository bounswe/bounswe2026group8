package com.bounswe2026group8.emergencyhub.map.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R

class OfflineFeaturesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_features)

        val mapButton = findViewById<Button>(R.id.btnOpenMap)
        val checklistButton = findViewById<Button>(R.id.btnChecklist)
        val contactsButton = findViewById<Button>(R.id.btnContacts)

        mapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        checklistButton.setOnClickListener {
            Toast.makeText(this, "Emergency Checklist — coming soon!", Toast.LENGTH_SHORT).show()
        }

        contactsButton.setOnClickListener {
            Toast.makeText(this, "Offline Contacts — coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}

