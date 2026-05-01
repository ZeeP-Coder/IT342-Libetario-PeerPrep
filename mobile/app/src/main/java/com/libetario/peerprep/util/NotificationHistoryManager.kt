package com.libetario.peerprep.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class NotificationItem(
    val title: String,
    val message: String,
    val timestamp: Long
)

object NotificationHistoryManager {
    private const val PREF_NAME = "notification_history"
    private const val KEY_HISTORY_PREFIX = "history_"
    private val gson = Gson()

    private fun getHistoryKey(email: String): String = KEY_HISTORY_PREFIX + email

    fun saveNotification(context: Context, userEmail: String, title: String, message: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = getHistoryKey(userEmail)
        val history = getHistory(context, userEmail).toMutableList()
        
        // Add new notification at the top
        history.add(0, NotificationItem(title, message, System.currentTimeMillis()))
        
        // Keep only last 20 notifications
        val limitedHistory = if (history.size > 20) history.take(20) else history
        
        prefs.edit().putString(key, gson.toJson(limitedHistory)).apply()
    }

    fun getHistory(context: Context, userEmail: String): List<NotificationItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = getHistoryKey(userEmail)
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NotificationItem>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory(context: Context, userEmail: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(getHistoryKey(userEmail)).apply()
    }
}
