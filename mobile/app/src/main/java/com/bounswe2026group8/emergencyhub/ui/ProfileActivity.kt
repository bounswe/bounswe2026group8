package com.bounswe2026group8.emergencyhub.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.ExpertiseFieldCreateRequest
import com.bounswe2026group8.emergencyhub.api.ExpertiseFieldData
import com.bounswe2026group8.emergencyhub.api.ProfileData
import com.bounswe2026group8.emergencyhub.api.ProfileUpdateRequest
import com.bounswe2026group8.emergencyhub.api.ResourceCreateRequest
import com.bounswe2026group8.emergencyhub.api.ResourceData
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private val bloodTypeOptions = arrayOf("—", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    private lateinit var tokenManager: TokenManager
    private var currentProfile: ProfileData? = null
    private var ignoreBloodTypeSelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tokenManager = TokenManager(this)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        loadIdentity()
        loadProfile()
        loadResources()
        setupResourceForm()

        val user = tokenManager.getUser()
        if (user?.role == "EXPERT") {
            findViewById<View>(R.id.cardExpertise).visibility = View.VISIBLE
            loadExpertise()
            setupExpertiseForm()
        }
    }

    // ── Identity card ───────────────────────────────────────────────────────────

    private fun loadIdentity() {
        val user = tokenManager.getUser() ?: return
        findViewById<TextView>(R.id.txtAvatar).text =
            user.fullName.firstOrNull()?.uppercase() ?: "?"
        findViewById<TextView>(R.id.txtFullName).text = user.fullName
        findViewById<TextView>(R.id.txtEmail).text = user.email
        findViewById<TextView>(R.id.txtRoleBadge).text =
            if (user.role == "EXPERT") "Expert" else "Standard"
    }

    // ── Profile ─────────────────────────────────────────────────────────────────

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).getProfile()
                if (res.isSuccessful) {
                    currentProfile = res.body()!!
                    displayProfile(currentProfile!!)
                }
            } catch (_: Exception) { }
        }
    }

    private fun displayProfile(p: ProfileData) {
        setField(R.id.fieldPhone, getString(R.string.profile_phone), p.phoneNumber)
        setField(R.id.fieldEmergencyContact, getString(R.string.profile_emergency_contact), p.emergencyContact)
        setField(R.id.fieldEmergencyPhone, getString(R.string.profile_emergency_phone), p.emergencyContactPhone)
        setField(R.id.fieldBio, getString(R.string.profile_bio), p.bio)
        setField(R.id.fieldSpecialNeeds, getString(R.string.profile_special_needs_label), p.specialNeeds)

        setupBloodTypeSpinner(p.bloodType)

        val switchDisability = findViewById<MaterialSwitch>(R.id.switchDisability)
        switchDisability.isChecked = p.hasDisability
        switchDisability.setOnCheckedChangeListener { _, isChecked ->
            updateProfile(ProfileUpdateRequest(hasDisability = isChecked))
        }

        updateStatusButtons(p.availabilityStatus)
        setupStatusButtons()

        val statusBadge = findViewById<TextView>(R.id.txtStatusBadge)
        updateStatusBadgeText(statusBadge, p.availabilityStatus)
    }

    private fun setupBloodTypeSpinner(currentValue: String?) {
        val spinner = findViewById<Spinner>(R.id.spinnerBloodType)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypeOptions)
        spinner.adapter = adapter

        val idx = bloodTypeOptions.indexOf(currentValue ?: "").coerceAtLeast(0)
        ignoreBloodTypeSelection = true
        spinner.setSelection(idx)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (ignoreBloodTypeSelection) {
                    ignoreBloodTypeSelection = false
                    return
                }
                val selected = bloodTypeOptions[pos]
                val value = if (selected == "—") null else selected
                updateProfile(ProfileUpdateRequest(bloodType = value))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setField(includeId: Int, label: String, value: String?) {
        val view = findViewById<View>(includeId)
        view.findViewById<TextView>(R.id.fieldLabel).text = label
        view.findViewById<TextView>(R.id.fieldValue).text = value ?: "—"
        view.setOnClickListener { promptEdit(label, value ?: "") { newVal -> onFieldEdited(includeId, label, newVal) } }
    }

    private fun onFieldEdited(includeId: Int, label: String, newVal: String) {
        val req = when (includeId) {
            R.id.fieldPhone -> ProfileUpdateRequest(phoneNumber = newVal.ifBlank { null })
            R.id.fieldEmergencyContact -> ProfileUpdateRequest(emergencyContact = newVal.ifBlank { null })
            R.id.fieldEmergencyPhone -> ProfileUpdateRequest(emergencyContactPhone = newVal.ifBlank { null })
            R.id.fieldBio -> ProfileUpdateRequest(bio = newVal.ifBlank { null })
            R.id.fieldSpecialNeeds -> ProfileUpdateRequest(specialNeeds = newVal.ifBlank { null })
            else -> return
        }
        findViewById<View>(includeId).findViewById<TextView>(R.id.fieldValue).text = newVal.ifBlank { "—" }
        updateProfile(req)
    }

    private fun promptEdit(label: String, currentValue: String, onSave: (String) -> Unit) {
        val input = TextInputEditText(this)
        input.setText(currentValue)
        input.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        input.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_input))
        input.setPadding(32, 24, 32, 24)

        val dialog = android.app.AlertDialog.Builder(this, R.style.Theme_EmergencyHub)
            .setTitle(label)
            .setView(input)
            .setPositiveButton("Save") { _, _ -> onSave(input.text?.toString()?.trim() ?: "") }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun setupStatusButtons() {
        val btnSafe = findViewById<MaterialButton>(R.id.btnStatusSafe)
        val btnNeeds = findViewById<MaterialButton>(R.id.btnStatusNeedsHelp)
        val btnAvailable = findViewById<MaterialButton>(R.id.btnStatusAvailable)

        btnSafe.setOnClickListener { selectStatus("SAFE") }
        btnNeeds.setOnClickListener { selectStatus("NEEDS_HELP") }
        btnAvailable.setOnClickListener { selectStatus("AVAILABLE_TO_HELP") }
    }

    private fun selectStatus(status: String) {
        updateStatusButtons(status)
        updateStatusBadgeText(findViewById(R.id.txtStatusBadge), status)
        updateProfile(ProfileUpdateRequest(availabilityStatus = status))
    }

    private fun updateStatusButtons(active: String) {
        val buttons = mapOf(
            "SAFE" to R.id.btnStatusSafe,
            "NEEDS_HELP" to R.id.btnStatusNeedsHelp,
            "AVAILABLE_TO_HELP" to R.id.btnStatusAvailable
        )
        val colors = mapOf(
            "SAFE" to R.color.success,
            "NEEDS_HELP" to R.color.error,
            "AVAILABLE_TO_HELP" to R.color.accent
        )
        for ((key, id) in buttons) {
            val btn = findViewById<MaterialButton>(id)
            if (key == active) {
                val c = ContextCompat.getColor(this, colors[key]!!)
                btn.setTextColor(c)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(c)
            } else {
                btn.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                btn.strokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.border)
                )
            }
        }
    }

    private fun updateStatusBadgeText(badge: TextView, status: String) {
        val (label, colorRes) = when (status) {
            "NEEDS_HELP" -> "Needs Help" to R.color.error
            "AVAILABLE_TO_HELP" -> "Available" to R.color.accent
            else -> "Safe" to R.color.success
        }
        badge.text = "● $label"
        badge.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun updateProfile(req: ProfileUpdateRequest) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).updateProfile(req)
                if (res.isSuccessful) {
                    currentProfile = res.body()
                    toast("Saved")
                } else {
                    toast("Update failed")
                }
            } catch (_: Exception) {
                toast("Network error")
            }
        }
    }

    // ── Resources ───────────────────────────────────────────────────────────────

    private fun loadResources() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).getResources()
                if (res.isSuccessful) displayResources(res.body() ?: emptyList())
            } catch (_: Exception) { }
        }
    }

    private fun displayResources(list: List<ResourceData>) {
        val container = findViewById<LinearLayout>(R.id.resourceList)
        container.removeAllViews()
        val noItems = findViewById<TextView>(R.id.txtNoResources)
        noItems.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        for (item in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_input))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 8
                layoutParams = lp
            }

            val label = TextView(this).apply {
                text = "${item.name} · ${item.category} · x${item.quantity}"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val condBadge = TextView(this).apply {
                text = if (item.condition) "✓" else "✗"
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (item.condition) R.color.success else R.color.error
                    )
                )
                textSize = 16f
                setPadding(16, 0, 16, 0)
            }

            val deleteBtn = TextView(this).apply {
                text = "✕"
                setTextColor(ContextCompat.getColor(context, R.color.error))
                textSize = 16f
                setOnClickListener { deleteResource(item.id) }
            }

            row.addView(label)
            row.addView(condBadge)
            row.addView(deleteBtn)
            container.addView(row)
        }
    }

    private fun setupResourceForm() {
        val form = findViewById<LinearLayout>(R.id.formAddResource)
        val btnToggle = findViewById<MaterialButton>(R.id.btnAddResource)
        btnToggle.setOnClickListener {
            form.visibility = if (form.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        findViewById<MaterialButton>(R.id.btnSaveResource).setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.inputResourceName).text?.toString()?.trim() ?: ""
            val cat = findViewById<TextInputEditText>(R.id.inputResourceCategory).text?.toString()?.trim() ?: ""
            val qty = findViewById<TextInputEditText>(R.id.inputResourceQty).text?.toString()?.toIntOrNull() ?: 1
            if (name.isBlank() || cat.isBlank()) {
                toast("Name and category are required")
                return@setOnClickListener
            }
            createResource(ResourceCreateRequest(name, cat, qty))
        }
    }

    private fun createResource(req: ResourceCreateRequest) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).createResource(req)
                if (res.isSuccessful) {
                    toast("Resource added")
                    findViewById<TextInputEditText>(R.id.inputResourceName).text?.clear()
                    findViewById<TextInputEditText>(R.id.inputResourceCategory).text?.clear()
                    findViewById<TextInputEditText>(R.id.inputResourceQty).setText("1")
                    findViewById<LinearLayout>(R.id.formAddResource).visibility = View.GONE
                    loadResources()
                } else {
                    toast("Failed to add resource")
                }
            } catch (_: Exception) {
                toast("Network error")
            }
        }
    }

    private fun deleteResource(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).deleteResource(id)
                if (res.isSuccessful) {
                    toast("Removed")
                    loadResources()
                }
            } catch (_: Exception) { }
        }
    }

    // ── Expertise ───────────────────────────────────────────────────────────────

    private fun loadExpertise() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).getExpertiseFields()
                if (res.isSuccessful) displayExpertise(res.body() ?: emptyList())
            } catch (_: Exception) { }
        }
    }

    private fun displayExpertise(list: List<ExpertiseFieldData>) {
        val container = findViewById<LinearLayout>(R.id.expertiseList)
        container.removeAllViews()
        val noItems = findViewById<TextView>(R.id.txtNoExpertise)
        noItems.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        for (item in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_input))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 8
                layoutParams = lp
            }

            val label = TextView(this).apply {
                text = "${item.field} · ${if (item.certificationLevel == "ADVANCED") "★ Advanced" else "◎ Beginner"}"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val deleteBtn = TextView(this).apply {
                text = "✕"
                setTextColor(ContextCompat.getColor(context, R.color.error))
                textSize = 16f
                setOnClickListener { deleteExpertise(item.id) }
            }

            row.addView(label)
            row.addView(deleteBtn)
            container.addView(row)
        }
    }

    private fun setupExpertiseForm() {
        val form = findViewById<LinearLayout>(R.id.formAddExpertise)
        val btnToggle = findViewById<MaterialButton>(R.id.btnAddExpertise)
        btnToggle.setOnClickListener {
            form.visibility = if (form.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val spinner = findViewById<Spinner>(R.id.spinnerCertLevel)
        val levels = arrayOf("Beginner", "Advanced")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, levels)

        findViewById<MaterialButton>(R.id.btnSaveExpertise).setOnClickListener {
            val field = findViewById<TextInputEditText>(R.id.inputExpertiseField).text?.toString()?.trim() ?: ""
            val level = if (spinner.selectedItemPosition == 1) "ADVANCED" else "BEGINNER"
            val url = findViewById<TextInputEditText>(R.id.inputCertUrl).text?.toString()?.trim()
            if (field.isBlank()) {
                toast("Field name is required")
                return@setOnClickListener
            }
            createExpertise(ExpertiseFieldCreateRequest(field, level, url?.ifBlank { null }))
        }
    }

    private fun createExpertise(req: ExpertiseFieldCreateRequest) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).createExpertiseField(req)
                if (res.isSuccessful) {
                    toast("Expertise added")
                    findViewById<TextInputEditText>(R.id.inputExpertiseField).text?.clear()
                    findViewById<TextInputEditText>(R.id.inputCertUrl).text?.clear()
                    findViewById<LinearLayout>(R.id.formAddExpertise).visibility = View.GONE
                    loadExpertise()
                } else {
                    toast("Failed to add expertise")
                }
            } catch (_: Exception) {
                toast("Network error")
            }
        }
    }

    private fun deleteExpertise(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).deleteExpertiseField(id)
                if (res.isSuccessful) {
                    toast("Removed")
                    loadExpertise()
                }
            } catch (_: Exception) { }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
