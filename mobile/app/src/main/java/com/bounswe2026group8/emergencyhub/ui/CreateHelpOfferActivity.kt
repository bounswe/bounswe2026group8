package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.CreateHelpOffer
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Form screen for creating a new help offer.
 *
 * Collects skill/resource name, description, category, and availability.
 * On successful submission the user is returned to the list screen.
 */
class CreateHelpOfferActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var inputSkillOrResource: TextInputEditText
    private lateinit var inputDescription: TextInputEditText
    private lateinit var dropdownAvailability: AutoCompleteTextView
    private lateinit var dropdownCategory: AutoCompleteTextView
    private lateinit var btnSubmit: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_help_offer)

        tokenManager = TokenManager(this)

        // Auth guard
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        // Bind views
        inputSkillOrResource = findViewById(R.id.inputSkillOrResource)
        inputDescription = findViewById(R.id.inputDescription)
        dropdownAvailability = findViewById(R.id.dropdownAvailability)
        dropdownCategory = findViewById(R.id.dropdownCategory)
        btnSubmit = findViewById(R.id.btnSubmit)

        // Back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        // Category dropdown — no default so the hint shows as placeholder
        val categoryLabels = arrayOf("Medical", "Food", "Shelter", "Transport")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryLabels)
        dropdownCategory.setAdapter(categoryAdapter)

        // Availability dropdown
        val availabilityLabels = arrayOf("24/7", "Weekdays", "Weekends", "Mornings", "Evenings", "On-call")
        val availabilityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, availabilityLabels)
        dropdownAvailability.setAdapter(availabilityAdapter)

        // Submit
        btnSubmit.setOnClickListener { attemptSubmit() }
    }

    private fun attemptSubmit() {
        val skillOrResource = inputSkillOrResource.text.toString().trim()
        val description = inputDescription.text.toString().trim()
        val availability = dropdownAvailability.text.toString().trim()

        // Client-side validation
        if (skillOrResource.isEmpty()) {
            Toast.makeText(this, "Skill or resource is required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(this, "Description is required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (dropdownCategory.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            return
        }
        if (availability.isEmpty()) {
            Toast.makeText(this, "Availability is required.", Toast.LENGTH_SHORT).show()
            return
        }

        val category = when (dropdownCategory.text.toString()) {
            "Medical"   -> "MEDICAL"
            "Food"      -> "FOOD"
            "Shelter"   -> "SHELTER"
            "Transport" -> "TRANSPORT"
            else        -> "MEDICAL"
        }

        // Disable button while submitting
        btnSubmit.isEnabled = false
        btnSubmit.text = getString(R.string.submitting)

        val request = CreateHelpOffer(
            category = category,
            skillOrResource = skillOrResource,
            description = description,
            availability = availability
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@CreateHelpOfferActivity)
                    .createHelpOffer(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CreateHelpOfferActivity,
                        getString(R.string.help_offer_created),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    val errorText = response.errorBody()?.string() ?: "Submission failed."
                    Toast.makeText(
                        this@CreateHelpOfferActivity,
                        errorText,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateHelpOfferActivity,
                    "Network error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnSubmit.isEnabled = true
                btnSubmit.text = getString(R.string.submit)
            }
        }
    }

    private fun navigateToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
