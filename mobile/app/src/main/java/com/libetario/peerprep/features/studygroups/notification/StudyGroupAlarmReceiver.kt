package com.libetario.peerprep.features.studygroups.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.libetario.peerprep.R
import com.libetario.peerprep.features.studygroups.ui.HomeActivity
import com.libetario.peerprep.shared.session.SessionManager
import kotlin.math.abs

class StudyGroupAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "StudyGroupAlarmReceiver"
        const val CHANNEL_ID = "study_group_notifications"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_IS_REMINDER = "extra_is_reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: return
        val targetEmail = intent.getStringExtra(EXTRA_USER_EMAIL)
        val isReminder = intent.getBooleanExtra(EXTRA_IS_REMINDER, false)

        // BUG FIX: If the user is logged out, stop immediately
        val currentUser = SessionManager.getCurrentUser(context)
        if (currentUser == null) {
            Log.d(TAG, "User logged out. Ignoring alarm for $groupName")
            return
        }

        // BUG FIX: Ensure the notification is for the active user
        if (targetEmail != null && !currentUser.email.equals(targetEmail, ignoreCase = true)) {
            Log.d(TAG, "Alarm belongs to $targetEmail. Ignoring for active user ${currentUser.email}")
            return
        }

        createNotificationChannel(context)

        val title = if (isReminder) "Upcoming Study Session" else "Study Session Starting Now"
        val message = if (isReminder) "$groupName starts in 5 minutes!" else "It's time for $groupName!"

        // Save to history (Fixing missing email parameter)
        NotificationHistoryManager.saveNotification(context, currentUser.email!!, title, message)

        val homeIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, abs(groupName.hashCode()), homeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            // Notification ID: Use group hash + offset for reminders so both can show
            val notificationId = abs(groupName.hashCode()) + (if (isReminder) 1 else 0)
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Study Group Reminders"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders for scheduled study sessions"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
