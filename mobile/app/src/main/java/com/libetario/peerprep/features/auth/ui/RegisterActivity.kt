package com.libetario.peerprep.features.auth.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.libetario.peerprep.R
import com.libetario.peerprep.auth.SessionManager
import com.libetario.peerprep.features.auth.model.RegisterRequest
import com.libetario.peerprep.shared.api.RetrofitClient
import com.libetario.peerprep.features.studygroups.ui.HomeActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        sessionManager = SessionManager(this)

        val nameInput = findViewById<EditText>(R.id.et_name)
        val emailInput = findViewById<EditText>(R.id.et_email)
        val universityInput = findViewById<EditText>(R.id.et_university)
        val majorSpinner = findViewById<Spinner>(R.id.spinner_major)
        val passwordInput = findViewById<EditText>(R.id.et_password)
        val confirmPasswordInput = findViewById<EditText>(R.id.et_confirm_password)
        val registerBtn = findViewById<Button>(R.id.btn_register)
        val loginLink = findViewById<Button>(R.id.btn_login_link)

        registerBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val university = universityInput.text.toString().trim()
            val major = majorSpinner.selectedItem.toString()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (validateInputs(name, email, university, password, confirmPassword)) {
                registerUser(name, email, university, major, password)
            }
        }

        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(
        name: String,
        email: String,
        university: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            return false
        }
        if (university.isEmpty()) {
            Toast.makeText(this, "University is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerUser(
        name: String,
        email: String,
        university: String,
        major: String,
        password: String
    ) {
        Log.d(TAG, "registerUser() called with email: $email")

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Creating RegisterRequest...")
                val request = RegisterRequest(name, email, password, university, major)

                Log.d(TAG, "Sending to backend: POST /api/auth/register")
                val response = RetrofitClient.authService.register(request)

                Log.d(TAG, "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    Log.d(TAG, "Registration successful")
                    if (authResponse != null && authResponse.email != null && authResponse.fullName != null) {
                        sessionManager.saveUserSession(
                            authResponse.email,
                            authResponse.fullName,
                            authResponse.accessToken ?: ""
                        )
                    }
                    Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed (code: ${response.code()})"
                    Log.e(TAG, "Registration error: $errorMsg")
                    Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration exception: ${e.message}", e)
                Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}