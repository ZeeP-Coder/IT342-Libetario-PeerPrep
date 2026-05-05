package com.libetario.peerprep.features.studygroups.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.libetario.peerprep.R
import com.libetario.peerprep.features.profile.ui.ProfileActivity
import com.libetario.peerprep.features.studygroups.adapter.NotificationHistoryAdapter
import com.libetario.peerprep.features.studygroups.adapter.StudyGroupAdapter
import com.libetario.peerprep.features.studygroups.adapter.StudyPartnerAdapter
import com.libetario.peerprep.features.studygroups.model.JoinLeaveRequest
import com.libetario.peerprep.features.studygroups.model.StudyGroupDashboard
import com.libetario.peerprep.features.studygroups.notification.NotificationHistoryManager
import com.libetario.peerprep.features.studygroups.notification.NotificationScheduler
import com.libetario.peerprep.ui.LandingActivity
import com.libetario.peerprep.shared.api.RetrofitClient
import com.libetario.peerprep.shared.session.SessionManager
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
        private const val REQUEST_CODE_REFRESH = 1001
        private const val PREF_SHOW_PROFILE_POPUP = "show_profile_popup_v1"
    }

    private var currentTab: String = "available"
    private var availableQuery: String = ""
    private var myGroupsQuery: String = ""
    private var partnersQuery: String = ""

    private var dashboard: StudyGroupDashboard? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvContentTitle: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var searchView: SearchView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            dashboard?.myStudyGroups?.let { NotificationScheduler.scheduleNotificationsForGroups(this, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val currentUser = SessionManager.getCurrentUser(this)
        if (currentUser == null || currentUser.email == null) {
            SessionManager.clearCurrentUser(this)
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
            return
        }

        initViews()
        setupListeners()

        findViewById<TextView>(R.id.tv_header_user_name).text = currentUser.name ?: "PeerPrep"
        findViewById<TextView>(R.id.tv_welcome_title).text = getString(R.string.welcome_back_format, currentUser.name ?: "User")

        loadDashboard(currentUser.email!!)
        checkProfileCompletion(currentUser.email!!)
        checkNotificationPermission()

        // Ensure Dashboard is selected by default in footer
        bottomNav.selectedItemId = R.id.nav_dashboard
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rv_content)
        loadingOverlay = findViewById(R.id.loading_overlay)
        tvContentTitle = findViewById(R.id.tv_content_title)
        tabLayout = findViewById(R.id.tab_layout)
        bottomNav = findViewById(R.id.bottom_navigation)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        searchView = findViewById(R.id.search_view)

        recyclerView.layoutManager = LinearLayoutManager(this)
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.pp_primary))
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            SessionManager.getCurrentUser(this)?.email?.let { loadDashboard(it) } ?: run { swipeRefresh.isRefreshing = false }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                updateQuery(query ?: "")
                renderTab(currentTab)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                updateQuery(newText ?: "")
                renderTab(currentTab)
                return true
            }
        })

        findViewById<MaterialCardView>(R.id.card_available_groups).setOnClickListener { tabLayout.getTabAt(0)?.select() }
        findViewById<MaterialCardView>(R.id.card_my_groups).setOnClickListener { tabLayout.getTabAt(1)?.select() }
        findViewById<MaterialCardView>(R.id.card_partners).setOnClickListener { tabLayout.getTabAt(2)?.select() }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val newTab = when (tab?.position) {
                    0 -> "available"
                    1 -> "my"
                    2 -> "partners"
                    else -> "available"
                }
                switchTab(newTab)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Sync: Clicking Dashboard footer icon always resets to the first tab
                    if (tabLayout.selectedTabPosition != 0) {
                        tabLayout.getTabAt(0)?.select()
                    }
                    true // Highlighting Dashboard
                }
                R.id.nav_create -> {
                    startActivityForResult(Intent(this, CreateGroupActivity::class.java), REQUEST_CODE_REFRESH)
                    false // Action only, don't highlight
                }
                R.id.nav_notifications -> {
                    showNotifications()
                    false // Action only, don't highlight
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false // Action only, don't highlight
                }
                R.id.nav_logout -> {
                    showLogoutConfirmation()
                    false // Action only, don't highlight
                }
                else -> false
            }
        }
    }

    private fun updateQuery(query: String) {
        when (currentTab) {
            "available" -> availableQuery = query
            "my" -> myGroupsQuery = query
            "partners" -> partnersQuery = query
        }
    }

    private fun getActiveQuery(): String {
        return when (currentTab) {
            "available" -> availableQuery
            "my" -> myGroupsQuery
            "partners" -> partnersQuery
            else -> ""
        }
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        updateTabStyles()

        // Sync: Ensure "Dashboard" stays glowing in footer while we switch content tabs
        if (bottomNav.selectedItemId != R.id.nav_dashboard) {
            bottomNav.selectedItemId = R.id.nav_dashboard
        }

        searchView.setQuery(getActiveQuery(), false)
        renderTab(tab)
    }

    private fun renderTab(tab: String) {
        val db = dashboard ?: return
        val query = getActiveQuery().trim()

        when (tab) {
            "available" -> {
                val filtered = db.availableStudyGroups.filter { group ->
                    query.isEmpty() ||
                            group.subject.contains(query, ignoreCase = true) ||
                            group.description.contains(query, ignoreCase = true) ||
                            group.location.contains(query, ignoreCase = true)
                }
                tvContentTitle.text = getString(R.string.available_groups_title, filtered.size)
                recyclerView.adapter = StudyGroupAdapter(filtered, false, { id -> showGroupDetailsById(id) }, { id -> joinGroupById(id) }, {}, {})
            }
            "my" -> {
                val filtered = db.myStudyGroups.filter { group ->
                    query.isEmpty() ||
                            group.subject.contains(query, ignoreCase = true) ||
                            group.description.contains(query, ignoreCase = true) ||
                            group.location.contains(query, ignoreCase = true)
                }
                tvContentTitle.text = getString(R.string.my_groups_title, filtered.size)
                recyclerView.adapter = StudyGroupAdapter(filtered, true, { id -> showGroupDetailsById(id, true) }, {}, { id -> leaveGroupById(id) }, { id -> deleteGroupById(id) })
            }
            "partners" -> {
                val filtered = db.studyPartners.filter { partner ->
                    query.isEmpty() ||
                            partner.fullName.contains(query, ignoreCase = true) ||
                            partner.university.contains(query, ignoreCase = true) ||
                            partner.major.contains(query, ignoreCase = true)
                }
                tvContentTitle.text = getString(R.string.study_partners_title, filtered.size)
                recyclerView.adapter = StudyPartnerAdapter(filtered)
            }
        }
    }

    private fun loadDashboard(userEmail: String) {
        if (!swipeRefresh.isRefreshing) showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.studyGroupService.getDashboard(userEmail)
                if (response.isSuccessful) {
                    dashboard = response.body()
                    updateStats()
                    renderTab(currentTab)
                    dashboard?.myStudyGroups?.let { groups ->
                        NotificationScheduler.scheduleNotificationsForGroups(this@HomeActivity, groups)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Dashboard error", e)
            } finally {
                showLoading(false)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateStats() {
        dashboard?.let { db ->
            findViewById<TextView>(R.id.tv_available_groups_count).text = db.availableStudyGroups.size.toString()
            findViewById<TextView>(R.id.tv_my_groups_count).text = db.myStudyGroups.size.toString()
            findViewById<TextView>(R.id.tv_partners_count).text = db.studyPartners.size.toString()
            updateTabStyles()
        }
    }

    private fun updateTabStyles() {
        val primaryColor = ContextCompat.getColor(this, R.color.pp_primary)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val mutedColor = ContextCompat.getColor(this, R.color.pp_text_muted)
        val mainColor = ContextCompat.getColor(this, R.color.pp_text_main)

        fun updateCard(cardId: Int, isSelected: Boolean, countId: Int, labelId: Int, subtitleId: Int) {
            val card = findViewById<MaterialCardView>(cardId)
            if (isSelected) {
                card.setCardBackgroundColor(ColorStateList.valueOf(primaryColor))
                card.findViewById<TextView>(countId).setTextColor(whiteColor)
                card.findViewById<TextView>(labelId).setTextColor(whiteColor)
                card.findViewById<TextView>(subtitleId).setTextColor(ContextCompat.getColor(this, R.color.pp_bg))
            } else {
                card.setCardBackgroundColor(ColorStateList.valueOf(whiteColor))
                card.findViewById<TextView>(countId).setTextColor(mainColor)
                card.findViewById<TextView>(labelId).setTextColor(mutedColor)
                card.findViewById<TextView>(subtitleId).setTextColor(mutedColor)
            }
        }

        updateCard(R.id.card_available_groups, currentTab == "available", R.id.tv_available_groups_count, R.id.tv_available_groups_label, R.id.tv_available_groups_subtitle)
        updateCard(R.id.card_my_groups, currentTab == "my", R.id.tv_my_groups_count, R.id.tv_my_groups_label, R.id.tv_my_groups_subtitle)
        updateCard(R.id.card_partners, currentTab == "partners", R.id.tv_partners_count, R.id.tv_partners_label, R.id.tv_partners_subtitle)
    }

    private fun showGroupDetailsById(groupId: Long, isFromMy: Boolean = false) {
        val intent = Intent(this, GroupDetailsActivity::class.java).apply {
            putExtra("groupId", groupId)
            if (isFromMy) {
                putExtra("joinedOverride", true)
                dashboard?.myStudyGroups?.find { it.id == groupId }?.let { if (it.ownedByCurrentUser) putExtra("ownedByCurrentUser", true) }
            }
        }
        startActivityForResult(intent, REQUEST_CODE_REFRESH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_REFRESH) { SessionManager.getCurrentUser(this)?.email?.let { loadDashboard(it) } }
    }

    private fun joinGroupById(groupId: Long) {
        val email = SessionManager.getCurrentUser(this)?.email ?: return
        showLoading(true)
        lifecycleScope.launch {
            try { if (RetrofitClient.studyGroupService.joinStudyGroup(groupId, JoinLeaveRequest(email)).isSuccessful) loadDashboard(email) }
            finally { showLoading(false) }
        }
    }

    private fun leaveGroupById(groupId: Long) {
        AlertDialog.Builder(this).setTitle("Leave Group").setMessage("Leave this group?").setPositiveButton("Leave") { _, _ ->
            val email = SessionManager.getCurrentUser(this)?.email ?: return@setPositiveButton
            showLoading(true)
            lifecycleScope.launch {
                try { if (RetrofitClient.studyGroupService.leaveStudyGroup(groupId, JoinLeaveRequest(email)).isSuccessful) loadDashboard(email) }
                finally { showLoading(false) }
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun deleteGroupById(groupId: Long) {
        AlertDialog.Builder(this).setTitle("Delete Group").setMessage("Delete this group?").setPositiveButton("Delete") { _, _ ->
            val email = SessionManager.getCurrentUser(this)?.email ?: return@setPositiveButton
            showLoading(true)
            lifecycleScope.launch {
                try { if (RetrofitClient.studyGroupService.deleteStudyGroup(groupId, email).isSuccessful) loadDashboard(email) }
                finally { showLoading(false) }
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this).setTitle("Logout").setMessage("Logout?").setPositiveButton("Yes") { _, _ ->
            SessionManager.clearCurrentUser(this)
            startActivity(Intent(this, LandingActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
            finish()
        }.setNegativeButton("No", null).show()
    }

    private fun checkProfileCompletion(email: String) {
        val prefs = getSharedPreferences("PeerPrepPrefs", MODE_PRIVATE)
        if (prefs.getBoolean(PREF_SHOW_PROFILE_POPUP + "_" + email, false)) return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.userProfileService.getProfile(email)
                if (response.isSuccessful && response.body()?.googleAuth == true) {
                    val p = response.body()!!
                    val isMissingDetails = p.university.isNullOrBlank() || p.university.equals("Not Set", true) || p.major.isNullOrBlank() || p.major.equals("Not Set", true)
                    if (isMissingDetails) showCompleteProfileDialog(email)
                }
            } catch (e: Exception) { Log.e(TAG, "Profile completion check failed", e) }
        }
    }

    private fun showCompleteProfileDialog(email: String) {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_complete_profile, null)
        val d = AlertDialog.Builder(this, R.style.Theme_PeerPrep_Dialog).setView(v).setCancelable(true).create()
        val markAsShown = { getSharedPreferences("PeerPrepPrefs", MODE_PRIVATE).edit().putBoolean(PREF_SHOW_PROFILE_POPUP + "_" + email, true).apply() }
        v.findViewById<Button>(R.id.btn_later).setOnClickListener { markAsShown(); d.dismiss() }
        v.findViewById<Button>(R.id.btn_go_to_profile).setOnClickListener { markAsShown(); d.dismiss(); startActivity(Intent(this, ProfileActivity::class.java)) }
        d.show()
    }

    private fun showNotifications() {
        val currentUser = SessionManager.getCurrentUser(this)
        val email = currentUser?.email ?: return
        val history = NotificationHistoryManager.getHistory(this, email)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification_history, null)
        val rvHistory = dialogView.findViewById<RecyclerView>(R.id.rv_notif_history)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tv_empty_history)
        val btnClear = dialogView.findViewById<Button>(R.id.btn_clear_history)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close_history)

        if (history.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
            btnClear.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            btnClear.visibility = View.VISIBLE
            rvHistory.layoutManager = LinearLayoutManager(this)
            rvHistory.adapter = NotificationHistoryAdapter(history)
        }

        val dialog = AlertDialog.Builder(this, R.style.Theme_PeerPrep_Dialog).setView(dialogView).setCancelable(true).create()
        btnClear.setOnClickListener { NotificationHistoryManager.clearHistory(this, email); dialog.dismiss(); Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show() }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
}