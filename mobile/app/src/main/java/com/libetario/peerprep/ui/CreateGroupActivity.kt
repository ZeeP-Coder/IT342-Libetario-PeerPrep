package com.libetario.peerprep.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.libetario.peerprep.R
import com.libetario.peerprep.api.RetrofitClient
import com.libetario.peerprep.util.SessionManager
import com.libetario.peerprep.model.StudyGroupCreateRequest
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var etSubject: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etDay: AutoCompleteTextView
    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndTime: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etMaxMembers: TextInputEditText
    private lateinit var btnCreate: Button
    private lateinit var btnCancel: Button

    private val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Navigation icon was removed from XML, so we don't need to set listener for it
        // If it still shows up due to theme, we force it hidden
        toolbar.navigationIcon = null

        initViews()
        setupDayDropdown()
        setupTimePickers()

        btnCancel.setOnClickListener { finish() }
        btnCreate.setOnClickListener { createGroup() }
    }

    private fun initViews() {
        etSubject = findViewById(R.id.et_subject)
        etDescription = findViewById(R.id.et_description)
        etDay = findViewById(R.id.et_day)
        etStartTime = findViewById(R.id.et_start_time)
        etEndTime = findViewById(R.id.et_end_time)
        etLocation = findViewById(R.id.et_location)
        etMaxMembers = findViewById(R.id.et_max_members)
        btnCreate = findViewById(R.id.btn_create_group)
        btnCancel = findViewById(R.id.btn_cancel)
    }

    private fun setupDayDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, days)
        etDay.setAdapter(adapter)
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener {
            showTimePickerDialog(etStartTime)
        }
        etEndTime.setOnClickListener {
            showTimePickerDialog(etEndTime)
        }
    }

    private fun showTimePickerDialog(editText: TextInputEditText) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val amPm = if (selectedHour < 12) "AM" else "PM"
            val hourIn12Format = if (selectedHour % 12 == 0) 12 else selectedHour % 12
            val timeString = String.format("%02d:%02d %s", hourIn12Format, selectedMinute, amPm)
            editText.setText(timeString)
        }, hour, minute, false)

        timePickerDialog.show()
    }

    private fun createGroup() {
        val subject = etSubject.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val day = etDay.text.toString().trim()
        val startTime = etStartTime.text.toString().trim()
        val endTime = etEndTime.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val maxMembersStr = etMaxMembers.text.toString().trim()

        if (subject.isEmpty()) {
            etSubject.error = "Subject is required"
            return
        }
        if (day.isEmpty()) {
            etDay.error = "Day is required"
            return
        }
        if (startTime.isEmpty()) {
            etStartTime.error = "Start time is required"
            return
        }
        if (endTime.isEmpty()) {
            etEndTime.error = "End time is required"
            return
        }
        if (location.isEmpty()) {
            etLocation.error = "Location is required"
            return
        }
        
        val maxMembers = maxMembersStr.toIntOrNull() ?: 0
        if (maxMembers <= 0) {
            etMaxMembers.error = "Invalid member count"
            return
        }

        val currentUser = SessionManager.getCurrentUser(this)
        val email = currentUser?.email ?: run {
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Combine start and end time for the backend
        val fullTimeRange = "$startTime - $endTime"

        val req = StudyGroupCreateRequest(
            creatorEmail = email,
            subject = subject,
            description = description,
            day = day,
            meetingTime = fullTimeRange,
            location = location,
            maxMembers = maxMembers
        )

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.studyGroupService.createStudyGroup(req)
                if (resp.isSuccessful) {
                    Toast.makeText(this@CreateGroupActivity, "Study group created successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = resp.errorBody()?.string() ?: "Failed to create group"
                    Toast.makeText(this@CreateGroupActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateGroupActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
