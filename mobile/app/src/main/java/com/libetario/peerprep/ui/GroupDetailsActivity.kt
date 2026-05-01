package com.libetario.peerprep.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.libetario.peerprep.R
import com.libetario.peerprep.api.RetrofitClient
import com.libetario.peerprep.util.SessionManager
import com.libetario.peerprep.model.JoinLeaveRequest
import com.libetario.peerprep.model.StudyGroup
import kotlinx.coroutines.launch

class GroupDetailsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GroupDetailsActivity"
    }

    private var selectedGroup: StudyGroup? = null
    private var groupId: Long = 0
    private var joinedOverride: Boolean = false
    private var ownedOverride: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_group_details)

        groupId = intent.getLongExtra("groupId", 0)
        joinedOverride = intent.getBooleanExtra("joinedOverride", false)
        ownedOverride = intent.getBooleanExtra("ownedByCurrentUser", false)

        if (groupId == 0L) {
            finish()
            return
        }

        findViewById<Button>(R.id.btn_dialog_close).setOnClickListener {
            finish()
        }

        loadGroupDetails()
    }

    private fun loadGroupDetails() {
        val userEmail = SessionManager.getCurrentUser(this)?.email ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.studyGroupService.getStudyGroup(groupId, userEmail)

                if (response.isSuccessful) {
                    val group = response.body()
                    if (group != null) {
                        selectedGroup = group
                        displayGroupDetails(group)
                    }
                } else {
                    showError("Failed to load group details")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading group details", e)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun displayGroupDetails(group: StudyGroup) {
        findViewById<TextView>(R.id.tv_dialog_title).text = group.subject
        findViewById<TextView>(R.id.tv_dialog_desc).text = group.description
        findViewById<TextView>(R.id.tv_dialog_day).text = group.day
        findViewById<TextView>(R.id.tv_dialog_time).text = group.meetingTime
        findViewById<TextView>(R.id.tv_dialog_location).text = group.location
        findViewById<TextView>(R.id.tv_dialog_members_count).text = "${group.currentMembers}/${group.maxMembers}"

        // Update members list
        val membersList = findViewById<LinearLayout>(R.id.ll_dialog_members_list)
        membersList.removeAllViews()
        group.memberNames.forEach { name ->
            val tv = TextView(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.member_item_height)
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(16, 0, 0, 0)
                setBackgroundResource(R.drawable.bg_input_field)
                textSize = 12f
                val params = layoutParams as LinearLayout.LayoutParams
                params.setMargins(0, 0, 0, 4)
            }
            membersList.addView(tv)
        }

        val btnAction: Button = findViewById(R.id.btn_dialog_action)

        val isJoined = if (joinedOverride) true else group.joined
        val isOwned = if (ownedOverride) true else group.ownedByCurrentUser

        if (isJoined) {
            if (isOwned) {
                btnAction.text = "Delete Group"
                btnAction.setOnClickListener { confirmDeleteGroup() }
            } else {
                btnAction.text = "Leave Group"
                btnAction.setOnClickListener { confirmLeaveGroup() }
            }
        } else {
            btnAction.text = if (group.joinable) "Join Group" else "Group Full"
            btnAction.isEnabled = group.joinable
            btnAction.setOnClickListener {
                if (group.joinable) joinGroup()
            }
        }
    }

    private fun joinGroup() {
        val userEmail = SessionManager.getCurrentUser(this)?.email ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.studyGroupService.joinStudyGroup(groupId, JoinLeaveRequest(userEmail))
                if (response.isSuccessful) {
                    Toast.makeText(this@GroupDetailsActivity, "Joined successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    loadGroupDetails()
                } else {
                    showError("Failed to join group")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun confirmLeaveGroup() {
        AlertDialog.Builder(this)
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Leave") { _, _ -> leaveGroup() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveGroup() {
        val userEmail = SessionManager.getCurrentUser(this)?.email ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.studyGroupService.leaveStudyGroup(groupId, JoinLeaveRequest(userEmail))
                if (response.isSuccessful) {
                    Toast.makeText(this@GroupDetailsActivity, "Left successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    showError("Failed to leave group")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun confirmDeleteGroup() {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete this group?")
            .setPositiveButton("Delete") { _, _ -> deleteGroup() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGroup() {
        val userEmail = SessionManager.getCurrentUser(this)?.email ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.studyGroupService.deleteStudyGroup(groupId, userEmail)
                if (response.isSuccessful) {
                    Toast.makeText(this@GroupDetailsActivity, "Group deleted!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    showError("Failed to delete group")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
