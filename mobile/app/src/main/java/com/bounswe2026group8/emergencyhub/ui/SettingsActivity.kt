package com.bounswe2026group8.emergencyhub.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.UserSettingsData
import com.bounswe2026group8.emergencyhub.api.UserSettingsUpdateRequest
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.util.ThemeManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private data class SettingSpec(
        val titleRes: Int,
        val descriptionRes: Int,
        val expertOnly: Boolean = false,
        val valueOf: (UserSettingsData) -> Boolean,
        val requestOf: (Boolean) -> UserSettingsUpdateRequest,
    )

    private lateinit var tokenManager: TokenManager
    private var currentSettings: UserSettingsData? = null
    private var bindingSettings = false
    private val switches = mutableMapOf<SettingSpec, MaterialSwitch>()

    private val notificationSpecs = listOf(
        SettingSpec(
            R.string.settings_notify_help_requests,
            R.string.settings_notify_help_requests_desc,
            valueOf = { it.notifyHelpRequests },
            requestOf = { UserSettingsUpdateRequest(notifyHelpRequests = it) },
        ),
        SettingSpec(
            R.string.settings_notify_urgent_posts,
            R.string.settings_notify_urgent_posts_desc,
            valueOf = { it.notifyUrgentPosts },
            requestOf = { UserSettingsUpdateRequest(notifyUrgentPosts = it) },
        ),
        SettingSpec(
            R.string.settings_notify_expertise_matches_only,
            R.string.settings_notify_expertise_matches_only_desc,
            expertOnly = true,
            valueOf = { it.notifyExpertiseMatchesOnly },
            requestOf = { UserSettingsUpdateRequest(notifyExpertiseMatchesOnly = it) },
        ),
    )

    private val privacySpecs = listOf(
        SettingSpec(
            R.string.settings_show_phone_number,
            R.string.settings_show_phone_number_desc,
            valueOf = { it.showPhoneNumber },
            requestOf = { UserSettingsUpdateRequest(showPhoneNumber = it) },
        ),
        SettingSpec(
            R.string.settings_show_emergency_contact,
            R.string.settings_show_emergency_contact_desc,
            valueOf = { it.showEmergencyContact },
            requestOf = { UserSettingsUpdateRequest(showEmergencyContact = it) },
        ),
        SettingSpec(
            R.string.settings_show_medical_info,
            R.string.settings_show_medical_info_desc,
            valueOf = { it.showMedicalInfo },
            requestOf = { UserSettingsUpdateRequest(showMedicalInfo = it) },
        ),
        SettingSpec(
            R.string.settings_show_availability_status,
            R.string.settings_show_availability_status_desc,
            valueOf = { it.showAvailabilityStatus },
            requestOf = { UserSettingsUpdateRequest(showAvailabilityStatus = it) },
        ),
        SettingSpec(
            R.string.settings_show_bio,
            R.string.settings_show_bio_desc,
            valueOf = { it.showBio },
            requestOf = { UserSettingsUpdateRequest(showBio = it) },
        ),
        SettingSpec(
            R.string.settings_show_location,
            R.string.settings_show_location_desc,
            valueOf = { it.showLocation },
            requestOf = { UserSettingsUpdateRequest(showLocation = it) },
        ),
        SettingSpec(
            R.string.settings_show_resources,
            R.string.settings_show_resources_desc,
            valueOf = { it.showResources },
            requestOf = { UserSettingsUpdateRequest(showResources = it) },
        ),
        SettingSpec(
            R.string.settings_show_expertise,
            R.string.settings_show_expertise_desc,
            expertOnly = true,
            valueOf = { it.showExpertise },
            requestOf = { UserSettingsUpdateRequest(showExpertise = it) },
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tokenManager = TokenManager(this)
        if (!tokenManager.isLoggedIn()) {
            finish()
            return
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        buildAppearanceRows()
        buildSettingRows()
        setSwitchesEnabled(false)
        loadSettings()
    }

    private fun buildAppearanceRows() {
        val appearanceRows = findViewById<LinearLayout>(R.id.appearanceRows)
        addThemeSwitchRow(appearanceRows)
    }

    private fun addThemeSwitchRow(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12.dp, 0, 12.dp)
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(this).apply {
            text = getString(R.string.settings_dark_mode)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val description = TextView(this).apply {
            text = getString(R.string.settings_dark_mode_desc)
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 12f
            setPadding(0, 4.dp, 16.dp, 0)
        }

        val switch = MaterialSwitch(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            contentDescription = getString(R.string.settings_dark_mode)
            isChecked = ThemeManager.isDarkMode(this@SettingsActivity)
            setOnCheckedChangeListener { _, checked ->
                ThemeManager.setDarkMode(this@SettingsActivity, checked)
            }
        }

        textColumn.addView(title)
        textColumn.addView(description)
        row.addView(textColumn)
        row.addView(switch)
        parent.addView(row)
    }

    private fun buildSettingRows() {
        val isExpert = tokenManager.getUser()?.role == "EXPERT"
        val notificationRows = findViewById<LinearLayout>(R.id.notificationRows)
        val privacyRows = findViewById<LinearLayout>(R.id.privacyRows)

        (notificationSpecs + privacySpecs)
            .filter { !it.expertOnly || isExpert }
            .forEach { spec ->
                val parent = if (spec in notificationSpecs) notificationRows else privacyRows
                addSwitchRow(parent, spec)
            }
    }

    private fun addSwitchRow(parent: LinearLayout, spec: SettingSpec) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12.dp, 0, 12.dp)
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(this).apply {
            text = getString(spec.titleRes)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val description = TextView(this).apply {
            text = getString(spec.descriptionRes)
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 12f
            setPadding(0, 4.dp, 16.dp, 0)
        }

        val switch = MaterialSwitch(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            contentDescription = getString(spec.titleRes)
            setOnCheckedChangeListener { _, checked ->
                if (!bindingSettings) updateSetting(spec, checked)
            }
        }

        textColumn.addView(title)
        textColumn.addView(description)
        row.addView(textColumn)
        row.addView(switch)
        parent.addView(row)
        switches[spec] = switch
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@SettingsActivity).getSettings()
                if (response.isSuccessful) {
                    currentSettings = response.body()
                    currentSettings?.let(::displaySettings)
                    setSwitchesEnabled(true)
                } else {
                    toast(getString(R.string.settings_load_failed))
                }
            } catch (_: Exception) {
                toast(getString(R.string.network_error))
            }
        }
    }

    private fun updateSetting(spec: SettingSpec, value: Boolean) {
        val switch = switches[spec] ?: return
        switch.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@SettingsActivity)
                    .updateSettings(spec.requestOf(value))
                if (response.isSuccessful) {
                    currentSettings = response.body()
                    currentSettings?.let(::displaySettings)
                    toast(getString(R.string.saved))
                } else {
                    restoreCurrentSettings()
                    toast(getString(R.string.settings_save_failed))
                }
            } catch (_: Exception) {
                restoreCurrentSettings()
                toast(getString(R.string.network_error))
            } finally {
                switch.isEnabled = true
            }
        }
    }

    private fun displaySettings(settings: UserSettingsData) {
        bindingSettings = true
        switches.forEach { (spec, switch) ->
            switch.isChecked = spec.valueOf(settings)
        }
        bindingSettings = false
    }

    private fun restoreCurrentSettings() {
        currentSettings?.let(::displaySettings)
    }

    private fun setSwitchesEnabled(enabled: Boolean) {
        switches.values.forEach { it.isEnabled = enabled }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
