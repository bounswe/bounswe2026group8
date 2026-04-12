package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.RegisterRequest
import com.bounswe2026group8.emergencyhub.viewmodel.SignUpViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
    private lateinit var spinnerHub: Spinner

    private val viewModel: SignUpViewModel by viewModels()

    private var selectedRole = "STANDARD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        inputFullName = findViewById(R.id.inputFullName)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        inputNeighborhood = findViewById(R.id.inputNeighborhood)
        inputExpertise = findViewById(R.id.inputExpertise)
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

        observeViewModel()
        viewModel.loadHubs()
    }

    private fun observeViewModel() {
        viewModel.hubs.observe(this) { hubs ->
            val hubNames = listOf(getString(R.string.select_hub)) + hubs.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hubNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerHub.adapter = adapter
        }

        viewModel.isLoading.observe(this) { loading ->
            btnSubmit.isEnabled = !loading
            btnSubmit.text = if (loading) getString(R.string.signing_up) else getString(R.string.sign_up)
        }

        viewModel.registerResult.observe(this) { result ->
            when (result) {
                is SignUpViewModel.RegisterResult.Success -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, SignInActivity::class.java))
                    finish()
                }
                is SignUpViewModel.RegisterResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getSelectedHubId(): Int? {
        val hubs = viewModel.hubs.value ?: emptyList()
        val pos = spinnerHub.selectedItemPosition
        return if (pos > 0 && pos <= hubs.size) hubs[pos - 1].id else null
    }

    private fun attemptRegister() {
        val fullName = inputFullName.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString()
        val confirmPassword = inputConfirmPassword.text.toString()
        val neighborhood = inputNeighborhood.text.toString().trim().ifEmpty { null }
        val expertise = inputExpertise.text.toString().trim().ifEmpty { null }
        val hubId = getSelectedHubId()

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

        val request = RegisterRequest(
            fullName = fullName,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            role = selectedRole,
            neighborhoodAddress = neighborhood,
            expertiseField = if (selectedRole == "EXPERT") expertise else null,
            hubId = hubId
        )

        viewModel.register(request)
    }
}
