package com.bounswe2026group8.emergencyhub.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.CreateHelpRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Form screen for creating a new help request.
 *
 * Collects title, description, category, urgency, and optional location
 * (text and/or GPS coordinates). On successful submission the user is
 * returned to the list screen with a success message.
 */
class CreateHelpRequestActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var inputTitle: TextInputEditText
    private lateinit var inputDescription: TextInputEditText
    private lateinit var inputLocationText: TextInputEditText
    private lateinit var dropdownCategory: AutoCompleteTextView
    private lateinit var dropdownUrgency: AutoCompleteTextView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnUseLocation: MaterialButton
    private lateinit var txtLocationStatus: TextView

    /** GPS coordinates captured via "Use My Location". */
    private var capturedLat: Double? = null
    private var capturedLng: Double? = null

    /** Launcher for the location permission request. */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_help_request)

        tokenManager = TokenManager(this)

        // Auth guard
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        // Bind views
        inputTitle = findViewById(R.id.inputTitle)
        inputDescription = findViewById(R.id.inputDescription)
        inputLocationText = findViewById(R.id.inputLocationText)
        dropdownCategory = findViewById(R.id.dropdownCategory)
        dropdownUrgency = findViewById(R.id.dropdownUrgency)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnUseLocation = findViewById(R.id.btnUseLocation)
        txtLocationStatus = findViewById(R.id.txtLocationStatus)

        // Back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        // Category dropdown — no default so the hint shows as placeholder
        val categoryLabels = arrayOf("Medical", "Food", "Shelter", "Transport")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryLabels)
        dropdownCategory.setAdapter(categoryAdapter)

        // Urgency dropdown — no default so the hint shows as placeholder
        val urgencyLabels = arrayOf("Low", "Medium", "High")
        val urgencyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, urgencyLabels)
        dropdownUrgency.setAdapter(urgencyAdapter)

        // GPS location
        btnUseLocation.setOnClickListener { requestLocation() }

        // Submit
        btnSubmit.setOnClickListener { attemptSubmit() }
    }

    // ── Location ─────────────────────────────────────────────────────────

    /** Checks permission and then fetches the device's current location. */
    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Retrieves the current GPS coordinates using the Fused Location Provider.
     * Requires ACCESS_FINE_LOCATION to already be granted.
     */
    @Suppress("MissingPermission")
    private fun fetchCurrentLocation() {
        btnUseLocation.isEnabled = false
        btnUseLocation.text = "Locating…"

        val client = LocationServices.getFusedLocationProviderClient(this)
        val cancellation = CancellationTokenSource()

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .addOnSuccessListener { location ->
                btnUseLocation.isEnabled = true
                btnUseLocation.text = getString(R.string.use_my_location)

                if (location != null) {
                    capturedLat = location.latitude
                    capturedLng = location.longitude
                    txtLocationStatus.text = getString(R.string.location_acquired)
                    txtLocationStatus.visibility = TextView.VISIBLE
                } else {
                    Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                btnUseLocation.isEnabled = true
                btnUseLocation.text = getString(R.string.use_my_location)
                Toast.makeText(this, "Location error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Form submission ──────────────────────────────────────────────────

    private fun attemptSubmit() {
        val title = inputTitle.text.toString().trim()
        val description = inputDescription.text.toString().trim()
        val locationText = inputLocationText.text.toString().trim().ifEmpty { null }

        // Client-side validation
        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required.", Toast.LENGTH_SHORT).show()
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
        if (dropdownUrgency.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select an urgency level.", Toast.LENGTH_SHORT).show()
            return
        }

        val category = when (dropdownCategory.text.toString()) {
            "Medical"   -> "MEDICAL"
            "Food"      -> "FOOD"
            "Shelter"   -> "SHELTER"
            "Transport" -> "TRANSPORT"
            else        -> "MEDICAL"
        }

        val urgency = when (dropdownUrgency.text.toString()) {
            "Low"    -> "LOW"
            "Medium" -> "MEDIUM"
            "High"   -> "HIGH"
            else     -> "LOW"
        }

        // Disable button while submitting
        btnSubmit.isEnabled = false
        btnSubmit.text = getString(R.string.submitting)

        val request = CreateHelpRequest(
            category = category,
            urgency = urgency,
            title = title,
            description = description,
            latitude = capturedLat?.let { "%.6f".format(it) },
            longitude = capturedLng?.let { "%.6f".format(it) },
            locationText = locationText
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@CreateHelpRequestActivity)
                    .createHelpRequest(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CreateHelpRequestActivity,
                        getString(R.string.help_request_created),
                        Toast.LENGTH_SHORT
                    ).show()
                    val detail = response.body()
                    if (detail != null) {
                        val detailIntent = Intent(
                            this@CreateHelpRequestActivity,
                            HelpRequestDetailActivity::class.java
                        )
                        detailIntent.putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, detail.id)
                        startActivity(detailIntent)
                    }
                    finish()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    // Show server validation errors
                    val errorText = response.errorBody()?.string() ?: "Submission failed."
                    Toast.makeText(
                        this@CreateHelpRequestActivity,
                        errorText,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateHelpRequestActivity,
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
