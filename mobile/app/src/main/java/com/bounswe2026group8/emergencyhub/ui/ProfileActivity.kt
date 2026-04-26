package com.bounswe2026group8.emergencyhub.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.ExpertiseCategoryData
import com.bounswe2026group8.emergencyhub.api.ExpertiseFieldCreateRequest
import com.bounswe2026group8.emergencyhub.api.ExpertiseFieldData
import com.bounswe2026group8.emergencyhub.api.ProfileData
import com.bounswe2026group8.emergencyhub.api.ProfileUpdateRequest
import com.bounswe2026group8.emergencyhub.api.ResourceCreateRequest
import com.bounswe2026group8.emergencyhub.api.ResourceData
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.util.LocaleManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private val bloodTypeOptions by lazy {
        arrayOf(getString(R.string.profile_empty_value), "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    }

    private lateinit var tokenManager: TokenManager
    private var currentProfile: ProfileData? = null
    private var ignoreBloodTypeSelection = false
    private var expertiseCategories: List<ExpertiseCategoryData> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tokenManager = TokenManager(this)
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        loadIdentity()
        loadProfile()
        loadResources()
        setupResourceForm()

        if (tokenManager.getUser()?.role == "EXPERT") {
            findViewById<View>(R.id.cardExpertise).visibility = View.VISIBLE
            loadExpertise()
            loadExpertiseCategories { setupExpertiseForm() }
        }
    }

    private fun loadIdentity() {
        val user = tokenManager.getUser() ?: return
        findViewById<TextView>(R.id.txtAvatar).text = user.fullName.firstOrNull()?.uppercase() ?: "?"
        findViewById<TextView>(R.id.txtFullName).text = user.fullName
        findViewById<TextView>(R.id.txtEmail).text = user.email
        findViewById<TextView>(R.id.txtRoleBadge).text =
            if (user.role == "EXPERT") getString(R.string.role_expert) else getString(R.string.role_standard)
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).getProfile()
                if (res.isSuccessful) {
                    currentProfile = res.body()!!
                    displayProfile(currentProfile!!)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun displayProfile(p: ProfileData) {
        setField(R.id.fieldPhone, getString(R.string.profile_phone), p.phoneNumber)
        setField(R.id.fieldEmergencyContact, getString(R.string.profile_emergency_contact), p.emergencyContact)
        setField(R.id.fieldEmergencyPhone, getString(R.string.profile_emergency_phone), p.emergencyContactPhone)
        setField(R.id.fieldBio, getString(R.string.profile_bio), p.bio)
        setField(R.id.fieldSpecialNeeds, getString(R.string.profile_special_needs_label), p.specialNeeds)

        setupBloodTypeSpinner(p.bloodType)

        findViewById<MaterialSwitch>(R.id.switchDisability).apply {
            isChecked = p.hasDisability
            setOnCheckedChangeListener { _, isChecked ->
                updateProfile(ProfileUpdateRequest(hasDisability = isChecked))
            }
        }

        updateStatusButtons(p.availabilityStatus)
        setupStatusButtons()
        updateStatusBadgeText(findViewById(R.id.txtStatusBadge), p.availabilityStatus)
    }

    private fun setupBloodTypeSpinner(currentValue: String?) {
        val spinner = findViewById<Spinner>(R.id.spinnerBloodType)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypeOptions)

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
                updateProfile(ProfileUpdateRequest(bloodType = if (selected == getString(R.string.profile_empty_value)) null else selected))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setField(includeId: Int, label: String, value: String?) {
        val view = findViewById<View>(includeId)
        view.findViewById<TextView>(R.id.fieldLabel).text = label
        view.findViewById<TextView>(R.id.fieldValue).text = value ?: getString(R.string.profile_empty_value)
        view.setOnClickListener { promptEdit(label, value ?: "") { newVal -> onFieldEdited(includeId, newVal) } }
    }

    private fun onFieldEdited(includeId: Int, newVal: String) {
        val req = when (includeId) {
            R.id.fieldPhone -> ProfileUpdateRequest(phoneNumber = newVal.ifBlank { null })
            R.id.fieldEmergencyContact -> ProfileUpdateRequest(emergencyContact = newVal.ifBlank { null })
            R.id.fieldEmergencyPhone -> ProfileUpdateRequest(emergencyContactPhone = newVal.ifBlank { null })
            R.id.fieldBio -> ProfileUpdateRequest(bio = newVal.ifBlank { null })
            R.id.fieldSpecialNeeds -> ProfileUpdateRequest(specialNeeds = newVal.ifBlank { null })
            else -> return
        }
        findViewById<View>(includeId).findViewById<TextView>(R.id.fieldValue).text =
            newVal.ifBlank { getString(R.string.profile_empty_value) }
        updateProfile(req)
    }

    private fun promptEdit(label: String, currentValue: String, onSave: (String) -> Unit) {
        val input = TextInputEditText(this)
        input.setText(currentValue)
        input.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        input.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_input))
        input.setPadding(32, 24, 32, 24)

        android.app.AlertDialog.Builder(this, R.style.Theme_EmergencyHub)
            .setTitle(label)
            .setView(input)
            .setPositiveButton(getString(R.string.profile_save)) { _, _ ->
                onSave(input.text?.toString()?.trim() ?: "")
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupStatusButtons() {
        findViewById<MaterialButton>(R.id.btnStatusSafe).setOnClickListener { selectStatus("SAFE") }
        findViewById<MaterialButton>(R.id.btnStatusNeedsHelp).setOnClickListener { selectStatus("NEEDS_HELP") }
        findViewById<MaterialButton>(R.id.btnStatusAvailable).setOnClickListener { selectStatus("AVAILABLE_TO_HELP") }
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
                btn.strokeColor = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.border))
            }
        }
    }

    private fun updateStatusBadgeText(badge: TextView, status: String) {
        val (label, colorRes) = when (status) {
            "NEEDS_HELP" -> getString(R.string.status_needs_help) to R.color.error
            "AVAILABLE_TO_HELP" -> getString(R.string.status_available) to R.color.accent
            else -> getString(R.string.status_safe) to R.color.success
        }
        badge.text = getString(R.string.profile_status_badge_format, label)
        badge.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun updateProfile(req: ProfileUpdateRequest) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).updateProfile(req)
                if (res.isSuccessful) {
                    currentProfile = res.body()
                    toast(getString(R.string.saved))
                } else {
                    toast(getString(R.string.profile_update_failed))
                }
            } catch (_: Exception) {
                toast(getString(R.string.network_error))
            }
        }
    }

    private fun loadResources() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).getResources()
                if (res.isSuccessful) displayResources(res.body() ?: emptyList())
            } catch (_: Exception) {
            }
        }
    }

    private fun displayResources(list: List<ResourceData>) {
        val container = findViewById<LinearLayout>(R.id.resourceList)
        val noItems = findViewById<TextView>(R.id.txtNoResources)
        container.removeAllViews()
        noItems.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        for (item in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_input))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            val label = TextView(this).apply {
                text = getString(R.string.profile_resource_item_format, item.name, item.category, item.quantity)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val condBadge = TextView(this).apply {
                text = if (item.condition) "\u2713" else "\u2717"
                setTextColor(ContextCompat.getColor(context, if (item.condition) R.color.success else R.color.error))
                textSize = 16f
                setPadding(16, 0, 16, 0)
            }

            val deleteBtn = TextView(this).apply {
                text = getString(R.string.delete_icon)
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
        findViewById<MaterialButton>(R.id.btnAddResource).setOnClickListener {
            form.visibility = if (form.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        findViewById<MaterialButton>(R.id.btnSaveResource).setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.inputResourceName).text?.toString()?.trim() ?: ""
            val cat = findViewById<TextInputEditText>(R.id.inputResourceCategory).text?.toString()?.trim() ?: ""
            val qty = findViewById<TextInputEditText>(R.id.inputResourceQty).text?.toString()?.toIntOrNull() ?: 1
            if (name.isBlank() || cat.isBlank()) {
                toast(getString(R.string.profile_resource_validation))
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
                    toast(getString(R.string.profile_resource_added))
                    findViewById<TextInputEditText>(R.id.inputResourceName).text?.clear()
                    findViewById<TextInputEditText>(R.id.inputResourceCategory).text?.clear()
                    findViewById<TextInputEditText>(R.id.inputResourceQty).setText("1")
                    findViewById<LinearLayout>(R.id.formAddResource).visibility = View.GONE
                    loadResources()
                } else {
                    toast(getString(R.string.profile_resource_add_failed))
                }
            } catch (_: Exception) {
                toast(getString(R.string.network_error))
            }
        }
    }

    private fun deleteResource(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).deleteResource(id)
                if (res.isSuccessful) {
                    toast(getString(R.string.removed))
                    loadResources()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun loadExpertise() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).getExpertiseFields()
                if (res.isSuccessful) displayExpertise(res.body() ?: emptyList())
            } catch (_: Exception) {
            }
        }
    }

    private fun displayExpertise(list: List<ExpertiseFieldData>) {
        val container = findViewById<LinearLayout>(R.id.expertiseList)
        val noItems = findViewById<TextView>(R.id.txtNoExpertise)
        container.removeAllViews()
        noItems.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        for (item in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_input))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            val level = if (item.certificationLevel == "ADVANCED") {
                getString(R.string.profile_cert_level_advanced)
            } else {
                getString(R.string.profile_cert_level_beginner)
            }
            val label = TextView(this).apply {
                text = getString(R.string.profile_expertise_item_format, item.category.displayName(LocaleManager.getLanguage(context)), level)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val deleteBtn = TextView(this).apply {
                text = getString(R.string.delete_icon)
                setTextColor(ContextCompat.getColor(context, R.color.error))
                textSize = 16f
                setOnClickListener { deleteExpertise(item.id) }
            }

            row.addView(label)
            row.addView(deleteBtn)
            container.addView(row)
        }
    }

    private fun loadExpertiseCategories(onDone: () -> Unit = {}) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).getExpertiseCategories()
                if (res.isSuccessful) expertiseCategories = res.body() ?: emptyList()
            } catch (_: Exception) { }
            onDone()
        }
    }

    private fun setupExpertiseForm() {
        val form = findViewById<LinearLayout>(R.id.formAddExpertise)
        findViewById<MaterialButton>(R.id.btnAddExpertise).setOnClickListener {
            form.visibility = if (form.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownExpertiseCategory)
        val langCode = LocaleManager.getLanguage(this)
        val categoryNames = expertiseCategories.map { it.displayName(langCode) }
        dropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames))
        var selectedCategoryId: Int? = null
        dropdown.setOnItemClickListener { _, _, pos, _ ->
            selectedCategoryId = expertiseCategories[pos].id
        }

        val spinner = findViewById<Spinner>(R.id.spinnerCertLevel)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(getString(R.string.profile_cert_level_beginner), getString(R.string.profile_cert_level_advanced))
        )

        findViewById<MaterialButton>(R.id.btnSaveExpertise).setOnClickListener {
            val catId = selectedCategoryId
            if (catId == null) {
                toast(getString(R.string.profile_expertise_validation))
                return@setOnClickListener
            }
            val level = if (spinner.selectedItemPosition == 1) "ADVANCED" else "BEGINNER"
            val url = findViewById<TextInputEditText>(R.id.inputCertUrl).text?.toString()?.trim()
            createExpertise(ExpertiseFieldCreateRequest(catId, level, url?.ifBlank { null }))
        }
    }

    private fun createExpertise(req: ExpertiseFieldCreateRequest) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).createExpertiseField(req)
                if (res.isSuccessful) {
                    toast(getString(R.string.profile_expertise_added))
                    findViewById<AutoCompleteTextView>(R.id.dropdownExpertiseCategory).text?.clear()
                    findViewById<TextInputEditText>(R.id.inputCertUrl).text?.clear()
                    findViewById<LinearLayout>(R.id.formAddExpertise).visibility = View.GONE
                    loadExpertise()
                } else {
                    toast(getString(R.string.profile_expertise_add_failed))
                }
            } catch (_: Exception) {
                toast(getString(R.string.network_error))
            }
        }
    }

    private fun deleteExpertise(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.getService(this@ProfileActivity).deleteExpertiseField(id)
                if (res.isSuccessful) {
                    toast(getString(R.string.removed))
                    loadExpertise()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
