package com.libetario.peerprep.features.profile.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.libetario.peerprep.R
import com.libetario.peerprep.features.profile.model.UpdateUserProfileRequest
import com.libetario.peerprep.shared.api.RetrofitClient
import com.libetario.peerprep.shared.model.User
import com.libetario.peerprep.shared.session.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etUniversity: EditText
    private lateinit var etMajor: AutoCompleteTextView
    private lateinit var btnSaveChanges: Button
    private lateinit var btnBack: Button
    private lateinit var tvAuthMethod: TextView
    private lateinit var tvAuthDesc: TextView

    private val majors = arrayOf(
        "Information Technology",
        "Computer Science",
        "Computer Engineering",
        "Software Engineering",
        "Data Science",
        "Information Systems",
        "Business Administration",
        "Civil Engineering",
        "Electrical Engineering",
        "Mechanical Engineering",
        "Psychology",
        "Nursing",
        "Accountancy",
        "Biology",
        "Mathematics"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        etName = findViewById(R.id.et_profile_name)
        etEmail = findViewById(R.id.et_profile_email)
        etUniversity = findViewById(R.id.et_profile_university)
        etMajor = findViewById(R.id.et_profile_major)
        btnSaveChanges = findViewById(R.id.btn_save_changes)
        btnBack = findViewById(R.id.btn_back_home)
        tvAuthMethod = findViewById(R.id.tv_auth_method)
        tvAuthDesc = findViewById(R.id.tv_auth_desc)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, majors)
        etMajor.setAdapter(adapter)

        val currentUser = SessionManager.getCurrentUser(this)
        if (currentUser != null) {
            etEmail.setText(currentUser.email)
            updateAuthDetails(currentUser.googleAuth)
            loadProfile(currentUser.email)
        } else {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnSaveChanges.setOnClickListener {
            saveProfile()
        }
    }

    private fun updateAuthDetails(isGoogle: Boolean) {
        if (isGoogle) {
            tvAuthMethod.text = "Authentication method: Google Sign-In"
            tvAuthDesc.text = "Google-authenticated users should complete profile fields so study partners can see your school and major."
        } else {
            tvAuthMethod.text = "Authentication method: Email + Password"
            tvAuthDesc.text = "Keep your profile updated so your study group details stay accurate."
        }
    }

    private fun loadProfile(email: String?) {
        if (email.isNullOrBlank()) {
            Toast.makeText(this, "Unable to load profile", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.userProfileService.getProfile(email)
                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        etName.setText(profile.fullName)
                        etEmail.setText(profile.email)
                        
                        // If values are "Not Set", leave them empty to show the hint
                        if (profile.university.equals("Not Set", ignoreCase = true)) {
                            etUniversity.setText("")
                        } else {
                            etUniversity.setText(profile.university)
                        }

                        if (profile.major.equals("Not Set", ignoreCase = true)) {
                            etMajor.setText("", false)
                        } else {
                            etMajor.setText(profile.major, false)
                        }
                        
                        updateAuthDetails(profile.googleAuth)

                        val updatedUser = User(
                            email = profile.email,
                            name = profile.fullName,
                            university = profile.university,
                            major = profile.major,
                            googleAuth = profile.googleAuth
                        )
                        SessionManager.saveUser(this@ProfileActivity, updatedUser)
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to load profile"
                    Toast.makeText(this@ProfileActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Load profile error", e)
                Toast.makeText(this@ProfileActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProfile() {
        val name = etName.text.toString().trim()
        val university = etUniversity.text.toString().trim()
        val major = etMajor.text.toString().trim()
        val email = etEmail.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Name is required"
            return
        }

        if (university.isEmpty()) {
            etUniversity.error = "University is required"
            return
        }

        if (major.isEmpty()) {
            etMajor.error = "Major is required"
            return
        }

        val currentUser = SessionManager.getCurrentUser(this)
        if (currentUser != null && email.isNotEmpty()) {
            val request = UpdateUserProfileRequest(
                email = email,
                fullName = name,
                university = university,
                major = major
            )

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.userProfileService.updateProfile(request)
                    if (response.isSuccessful) {
                        val profile = response.body()
                        val updatedUser = currentUser.copy(
                            name = profile?.fullName ?: name,
                            university = profile?.university ?: university,
                            major = profile?.major ?: major
                        )
                        SessionManager.saveUser(this@ProfileActivity, updatedUser)
                        Toast.makeText(this@ProfileActivity, "Profile updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Update failed"
                        Toast.makeText(this@ProfileActivity, "Server error: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Update error", e)
                    Toast.makeText(this@ProfileActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
