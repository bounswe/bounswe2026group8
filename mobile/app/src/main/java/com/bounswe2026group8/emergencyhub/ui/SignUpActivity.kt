package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.RegisterRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch

/**
 * Sign Up screen.
 *
 * Collects registration data matching the backend POST /register contract.
 * The expertise field is only shown when the EXPERT role is selected.
 * On success, navigates to Sign In.
 */
class SignUpActivity : AppCompatActivity() {

    private lateinit var inputFullName: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var inputNeighborhood: TextInputEditText
    private lateinit var inputExpertise: TextInputEditText
    private lateinit var expertiseLayout: TextInputLayout
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var btnSubmit: MaterialButton

    private var selectedRole = "STANDARD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Bind views
        inputFullName = findViewById(R.id.inputFullName)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        inputNeighborhood = findViewById(R.id.inputNeighborhood)
        inputExpertise = findViewById(R.id.inputExpertise)
        expertiseLayout = findViewById(R.id.expertiseLayout)
        toggleGroup = findViewById(R.id.roleToggleGroup)
        btnSubmit = findViewById(R.id.btnSignUp)

        // Role toggle — show expertise field only for EXPERT
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedRole = if (checkedId == R.id.btnRoleExpert) "EXPERT" else "STANDARD"
                expertiseLayout.visibility = if (selectedRole == "EXPERT") View.VISIBLE else View.GONE
            }
        }

        btnSubmit.setOnClickListener { attemptRegister() }

        // Navigate to Sign In
        findViewById<View>(R.id.linkSignIn).setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun attemptRegister() {
        val fullName = inputFullName.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString()
        val confirmPassword = inputConfirmPassword.text.toString()
        val neighborhood = inputNeighborhood.text.toString().trim().ifEmpty { null }
        val expertise = inputExpertise.text.toString().trim().ifEmpty { null }

        // ── Client-side validation ──────────────────────────────────────────
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedRole == "EXPERT" && expertise.isNullOrBlank()) {
            Toast.makeText(this, "Expertise field is required for Expert users.", Toast.LENGTH_SHORT).show()
            return
        }

        // ── API call ────────────────────────────────────────────────────────
        btnSubmit.isEnabled = false
        btnSubmit.text = getString(R.string.signing_up)

        val request = RegisterRequest(
            fullName = fullName,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            role = selectedRole,
            neighborhoodAddress = neighborhood,
            expertiseField = if (selectedRole == "EXPERT") expertise else null
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
