package com.libetario.peerprep.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.libetario.peerprep.model.StudyGroup
import com.libetario.peerprep.model.User
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    fun scheduleNotificationsForGroups(context: Context, groups: List<StudyGroup>) {
        val currentUser = SessionManager.getCurrentUser(context) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        for (group in groups) {
            val startTimeStr = group.meetingTime.split("-").firstOrNull()?.trim() ?: continue
            val calendar = getCalendarForGroup(group.day, startTimeStr) ?: continue

            val now = System.currentTimeMillis()
            val buffer = 10000 // 10s buffer

            // If time has passed, move to next week
            if (calendar.timeInMillis < now + buffer) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            // Schedule "Starting Now" (isReminder = false)
            scheduleAlarm(context, alarmManager, group, currentUser, calendar.timeInMillis, false)

            // Schedule "5 mins before" (isReminder = true)
            val reminderTime = calendar.timeInMillis - (5 * 60 * 1000)
            if (reminderTime > now + buffer) {
                scheduleAlarm(context, alarmManager, group, currentUser, reminderTime, true)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        group: StudyGroup,
        user: User,
        timeInMillis: Long,
        isReminder: Boolean
    ) {
        val intent = Intent(context, StudyGroupAlarmReceiver::class.java).apply {
            putExtra(StudyGroupAlarmReceiver.EXTRA_GROUP_NAME, group.subject)
            putExtra(StudyGroupAlarmReceiver.EXTRA_USER_EMAIL, user.email)
            putExtra(StudyGroupAlarmReceiver.EXTRA_IS_REMINDER, isReminder)
        }

        // Use absolute hash codes to prevent integer overflow from large IDs
        val baseId = abs(group.id.toString().hashCode())
        val requestCode = if (isReminder) baseId + 1 else baseId

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Alarm scheduling failed", e)
        }
    }

    private fun getCalendarForGroup(day: String, time: String): Calendar? {
        try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = sdf.parse(time) ?: return null
            
            val calendar = Calendar.getInstance()
            val timeCal = Calendar.getInstance().apply { this.time = date }
            
            calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val targetDay = when (day.lowercase()) {
                "monday" -> Calendar.MONDAY
                "tuesday" -> Calendar.TUESDAY
                "wednesday" -> Calendar.WEDNESDAY
                "thursday" -> Calendar.THURSDAY
                "friday" -> Calendar.FRIDAY
                "saturday" -> Calendar.SATURDAY
                "sunday" -> Calendar.SUNDAY
                else -> return null
            }
            
            while (calendar.get(Calendar.DAY_OF_WEEK) != targetDay) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            return calendar
        } catch (e: Exception) {
            return null
        }
    }
}
