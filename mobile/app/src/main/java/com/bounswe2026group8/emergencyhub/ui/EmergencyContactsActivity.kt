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

        contactsContainer = findViewById(R.id.contacts_container)
        database = AppDatabase.getDatabase(this)

        findViewById<Button>(R.id.btn_call_general).setOnClickListener { dialNumber("112") }
        findViewById<Button>(R.id.btn_add_custom_number).setOnClickListener { showAddContactDialog() }

        loadSavedContacts()
    }

    private fun loadSavedContacts() {
        lifecycleScope.launch(Dispatchers.IO) {
            val savedContacts = try {
                database.contactDao().getAllContacts()
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmergencyContactsActivity, getString(R.string.offline_contacts_load_failed), Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

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
            .setPositiveButton(getString(R.string.profile_save)) { dialog, _ ->
                val alias = etAlias.text.toString().trim()
                val number = etNumber.text.toString().trim()

                if (alias.isNotEmpty() && number.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            database.contactDao().insertContact(EmergencyContact(alias, number))
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@EmergencyContactsActivity, getString(R.string.offline_contacts_save_failed), Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        withContext(Dispatchers.Main) {
                            clearRenderedContacts()
                            loadSavedContacts()
                        }
                    }
                } else {
                    Toast.makeText(this, getString(R.string.offline_contacts_fill_both_fields), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun renderContactCard(contact: EmergencyContact) {
        val newCardView = LayoutInflater.from(this)
            .inflate(R.layout.item_emergency_contact, contactsContainer, false)

        newCardView.findViewById<TextView>(R.id.tv_contact_name).text = contact.alias
        newCardView.findViewById<TextView>(R.id.tv_contact_number).text = contact.phoneNumber
        newCardView.findViewById<Button>(R.id.btn_call).setOnClickListener { dialNumber(contact.phoneNumber) }
        newCardView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.offline_contacts_delete_title))
                .setMessage(getString(R.string.offline_contacts_delete_confirm, contact.alias))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            database.contactDao().deleteContact(contact)
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@EmergencyContactsActivity, getString(R.string.offline_contacts_delete_failed), Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            contactsContainer.removeView(newCardView)
                        }
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        contactsContainer.addView(newCardView, contactsContainer.childCount - 1)
    }

    private fun dialNumber(phoneNumber: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
    }

    private fun clearRenderedContacts() {
        val childCount = contactsContainer.childCount
        if (childCount > 3) {
            contactsContainer.removeViews(2, childCount - 3)
        }
    }
}
