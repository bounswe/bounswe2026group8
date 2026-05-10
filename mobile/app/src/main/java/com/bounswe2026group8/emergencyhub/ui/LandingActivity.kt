package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.map.ui.OfflineFeaturesActivity
import com.bounswe2026group8.emergencyhub.util.LocaleManager
import com.bounswe2026group8.emergencyhub.mesh.ui.MeshActivity
import com.google.android.material.button.MaterialButton

/**
 * Landing / Welcome screen.
 *
 * Shows the app name, a short description of the emergency preparedness
 * hub, and two buttons to navigate to Sign Up or Sign In.
 *
 * If the user already has a saved token, skips straight to the Dashboard.
 */
class LandingActivity : AppCompatActivity() {

    private lateinit var languageSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already logged in, go directly to Dashboard
        val tokenManager = TokenManager(this)
        if (tokenManager.isLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_landing)

        // Setup language selector
        setupLanguageSelector()

        findViewById<MaterialButton>(R.id.btnSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnSignIn).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnGuidedTour).setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnOfflineInfo).setOnClickListener {
            startActivity(Intent(this, OfflineFeaturesActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnMeshMessaging).setOnClickListener {
            startActivity(Intent(this, MeshActivity::class.java))
        }
    }

    private fun setupLanguageSelector() {
        languageSpinner = findViewById(R.id.spinnerLanguage)
        
        val languages = LocaleManager.getSupportedLanguages()
        val displayNames = languages.map { LocaleManager.getDisplayName(it) }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        // Set current selection
        val currentLanguage = LocaleManager.getLanguage(this)
        val position = languages.indexOf(currentLanguage)
        if (position >= 0) {
            languageSpinner.setSelection(position)
        }

        // Handle language change
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                val currentLanguage = LocaleManager.getLanguage(this@LandingActivity)
                if (selectedLanguage != currentLanguage) {
                    LocaleManager.setLanguage(this@LandingActivity, selectedLanguage)
                    LocaleManager.applyLocale(this@LandingActivity, selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
