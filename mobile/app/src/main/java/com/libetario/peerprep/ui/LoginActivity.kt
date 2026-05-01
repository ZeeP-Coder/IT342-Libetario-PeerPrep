package com.libetario.peerprep.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.libetario.peerprep.R
import com.libetario.peerprep.api.RetrofitClient
import com.libetario.peerprep.api.GoogleLoginRequest
import com.libetario.peerprep.model.LoginRequest
import com.libetario.peerprep.model.User
import com.libetario.peerprep.util.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 9001
    }

    // This matches the Client ID you provided
    private val serverClientId: String = "242891288998-bu2al00p956edtgepklq4f1ntoscitv2.apps.googleusercontent.com"
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (SessionManager.getCurrentUser(this) != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val emailInput = findViewById<EditText>(R.id.et_email)
        val passwordInput = findViewById<EditText>(R.id.et_password)
        val loginBtn = findViewById<Button>(R.id.btn_login)
        val signUpLink = findViewById<Button>(R.id.btn_signup_link)
        val googleBtn = findViewById<Button>(R.id.btn_google_placeholder)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (validateInputs(email, password)) loginUser(email, password)
        }

        googleBtn.setOnClickListener { googleSignIn() }
        signUpLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun googleSignIn() {
        // Sign out first to ensure account picker shows up if there was an error
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken

                if (idToken != null) {
                    sendGoogleTokenToBackend(idToken)
                } else {
                    Toast.makeText(this, "Google failed to provide an ID token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed code=${e.statusCode}", e)
                Toast.makeText(this, "Sign-in failed. Code: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendGoogleTokenToBackend(idToken: String) {
        lifecycleScope.launch {
            try {
                // Now using the GoogleLoginRequest data class for proper serialization
                val request = GoogleLoginRequest(idToken)
                val response = RetrofitClient.googleAuthService.googleLogin(request)
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null && authResponse.email != null) {
                        // Mark as Google Auth user
                        val user = User(
                            email = authResponse.email, 
                            name = authResponse.fullName,
                            googleAuth = true
                        )
                        SessionManager.saveUser(this@LoginActivity, user)
                        SessionManager.saveTokens(this@LoginActivity, authResponse.accessToken, authResponse.refreshToken)
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        val json = JSONObject(errorBody ?: "")
                        json.optString("message", json.optString("error", "Backend rejected login"))
                    } catch (e: Exception) {
                        "Server error: ${response.code()}"
                    }
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Backend rejected token: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        SessionManager.saveUser(this@LoginActivity, User(
                            email = authResponse.email, 
                            name = authResponse.fullName,
                            googleAuth = false
                        ))
                        SessionManager.saveTokens(this@LoginActivity, authResponse.accessToken, authResponse.refreshToken)
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
