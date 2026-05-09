package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.ExpertiseCategoryData
import com.bounswe2026group8.emergencyhub.api.Hub
import com.bounswe2026group8.emergencyhub.api.RegisterRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.util.LocaleManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var inputFullName: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var inputNeighborhood: TextInputEditText
    private lateinit var dropdownExpertise: AutoCompleteTextView
    private lateinit var expertiseLayout: TextInputLayout
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var btnSubmit: MaterialButton
    private lateinit var spinnerHub: Spinner

    private var selectedRole = "STANDARD"
    private var hubs: List<Hub> = emptyList()
    private var expertiseCategories: List<ExpertiseCategoryData> = emptyList()
    private var selectedCategoryId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        inputFullName = findViewById(R.id.inputFullName)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        inputNeighborhood = findViewById(R.id.inputNeighborhood)
        dropdownExpertise = findViewById(R.id.dropdownExpertiseCategory)
        expertiseLayout = findViewById(R.id.expertiseLayout)
        toggleGroup = findViewById(R.id.roleToggleGroup)
        btnSubmit = findViewById(R.id.btnSignUp)
        spinnerHub = findViewById(R.id.spinnerHub)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedRole = if (checkedId == R.id.btnRoleExpert) "EXPERT" else "STANDARD"
                expertiseLayout.visibility = if (selectedRole == "EXPERT") View.VISIBLE else View.GONE
            }
        }

        btnSubmit.setOnClickListener { attemptRegister() }

        findViewById<View>(R.id.linkSignIn).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        loadHubs()
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@SignUpActivity).getExpertiseCategories()
                if (response.isSuccessful) {
                    expertiseCategories = response.body() ?: emptyList()
                    val langCode = LocaleManager.getLanguage(this@SignUpActivity)
                    val names = expertiseCategories.map { it.displayName(langCode) }
                    dropdownExpertise.setAdapter(
                        ArrayAdapter(this@SignUpActivity, android.R.layout.simple_dropdown_item_1line, names)
                    )
                    dropdownExpertise.setOnItemClickListener { _, _, pos, _ ->
                        selectedCategoryId = expertiseCategories[pos].id
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadHubs() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@SignUpActivity).getHubs()
                if (response.isSuccessful) {
                    hubs = response.body() ?: emptyList()
                    val hubNames = listOf(getString(R.string.select_hub)) + hubs.map { it.name }
                    val adapter = ArrayAdapter(
                        this@SignUpActivity,
                        android.R.layout.simple_spinner_item,
                        hubNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerHub.adapter = adapter
                }
            } catch (_: Exception) {
                // Hub list failed to load — user can still sign up without hub
            }
        }
    }

    private fun getSelectedHubId(): Int? {
        val pos = spinnerHub.selectedItemPosition
        return if (pos > 0 && pos <= hubs.size) hubs[pos - 1].id else null
    }

    private fun attemptRegister() {
        val fullName = inputFullName.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString()
        val confirmPassword = inputConfirmPassword.text.toString()
        val neighborhood = inputNeighborhood.text.toString().trim().ifEmpty { null }
        val hubId = getSelectedHubId()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedRole == "EXPERT" && selectedCategoryId == null) {
            Toast.makeText(this, "Expertise category is required for Expert users.", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        btnSubmit.text = getString(R.string.signing_up)

        val request = RegisterRequest(
            fullName = fullName,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            role = selectedRole,
            neighborhoodAddress = neighborhood,
            categoryId = if (selectedRole == "EXPERT") selectedCategoryId else null,
            hubId = hubId
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@SignUpActivity).register(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@SignUpActivity, "Account created successfully!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SignUpActivity, SignInActivity::class.java))
                    finish()
                } else {
                    val errorBody = response.body()
                    val errorMsg = errorBody?.errors?.values?.flatten()?.joinToString("\n")
                        ?: errorBody?.message
                        ?: "Registration failed."
                    Toast.makeText(this@SignUpActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SignUpActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                btnSubmit.isEnabled = true
                btnSubmit.text = getString(R.string.sign_up)
            }
        }
    }
}
