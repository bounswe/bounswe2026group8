package com.bounswe2026group8.emergencyhub.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.viewmodel.SignInViewModel

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Sign In screen.
 *
 * Collects email + password, calls POST /login, stores the JWT token,
 * and navigates to the Dashboard on success.
 */
class SignInActivity : AppCompatActivity() {

    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var btnSignIn: MaterialButton

    private val viewModel: SignInViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnSignIn = findViewById(R.id.btnSignIn)

        btnSignIn.setOnClickListener { attemptLogin() }

        // Navigate to Sign Up
        findViewById<View>(R.id.linkSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        observeViewModel()
        requestNotificationPermission()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            btnSignIn.isEnabled = !loading
            btnSignIn.text = if (loading) getString(R.string.signing_in) else getString(R.string.sign_in)
        }

        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is SignInViewModel.LoginResult.Success -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                is SignInViewModel.LoginResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun attemptLogin() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.login(email, password)
    }
}
