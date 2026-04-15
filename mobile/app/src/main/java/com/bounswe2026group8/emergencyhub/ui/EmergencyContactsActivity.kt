package com.bounswe2026group8.emergencyhub.offline.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.offline.data.AppDatabase
import com.bounswe2026group8.emergencyhub.offline.data.EmergencyContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var contactsContainer: LinearLayout
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contacts)

        // 1. Initialize the UI container and Database
        contactsContainer = findViewById(R.id.contacts_container)
        database = AppDatabase.getDatabase(this)

        // 2. Set up the static buttons
        val btnCallGeneral = findViewById<Button>(R.id.btn_call_general)
        val btnAddCustom = findViewById<Button>(R.id.btn_add_custom_number)

        btnCallGeneral.setOnClickListener { dialNumber("112") }
        btnAddCustom.setOnClickListener { showAddContactDialog() }

        // 3. Load all saved custom contacts from the database when the screen opens
        loadSavedContacts()
    }

    private fun loadSavedContacts() {
        // Run database queries on a background thread (Dispatchers.IO)
        lifecycleScope.launch(Dispatchers.IO) {
            val savedContacts = try {
                database.contactDao().getAllContacts()
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EmergencyContactsActivity,
                        "Offline contacts could not be loaded. Please reopen the page.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            // Switch back to the Main UI thread to draw the cards on the screen
            withContext(Dispatchers.Main) {
                clearRenderedContacts()
                for (contact in savedContacts) {
                    renderContactCard(contact)
                }
            }
        }
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val etAlias = dialogView.findViewById<EditText>(R.id.et_alias)
        val etNumber = dialogView.findViewById<EditText>(R.id.et_number)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val alias = etAlias.text.toString().trim()
                val number = etNumber.text.toString().trim()

                if (alias.isNotEmpty() && number.isNotEmpty()) {
                    val newContact = EmergencyContact(alias, number)

                    // Save to the database in the background
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            database.contactDao().insertContact(newContact)
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@EmergencyContactsActivity,
                                    "Offline contacts could not be saved.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        // After saving, clear the current custom UI list and reload it
                        // so we get the accurate database ID for the new item
                        withContext(Dispatchers.Main) {
                            clearRenderedContacts()
                            loadSavedContacts()
                        }
                    }
                } else {
                    Toast.makeText(this, "Please fill out both fields", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun renderContactCard(contact: EmergencyContact) {
        val newCardView: View = LayoutInflater.from(this).inflate(R.layout.item_emergency_contact, contactsContainer, false)

        newCardView.findViewById<TextView>(R.id.tv_contact_name).text = contact.alias
        newCardView.findViewById<TextView>(R.id.tv_contact_number).text = contact.phoneNumber

        // Setup the CALL button
        newCardView.findViewById<Button>(R.id.btn_call).setOnClickListener {
            dialNumber(contact.phoneNumber)
        }

        // Setup the DELETE button
        newCardView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to remove ${contact.alias}?")
                .setPositiveButton("Delete") { _, _ ->

                    // Delete from the database in the background
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            database.contactDao().deleteContact(contact)
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@EmergencyContactsActivity,
                                    "Offline contact could not be deleted.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        // Remove the visual card from the screen on the main thread
                        withContext(Dispatchers.Main) {
                            contactsContainer.removeView(newCardView)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Insert the new card right above the "Add Custom Number" button
        val insertIndex = contactsContainer.childCount - 1
        contactsContainer.addView(newCardView, insertIndex)
    }

    private fun dialNumber(phoneNumber: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL)
        dialIntent.data = Uri.parse("tel:$phoneNumber")
        startActivity(dialIntent)
    }

    private fun clearRenderedContacts() {
        // Keep the title, 112 card, and "Add Custom Number" button in place.
        val childCount = contactsContainer.childCount
        if (childCount > 3) {
            contactsContainer.removeViews(2, childCount - 3)
        }
    }
}
