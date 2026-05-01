package com.libetario.peerprep.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.libetario.peerprep.model.User

object SessionManager {
    private const val TAG = "SessionManager"
    private const val PREF_NAME = "PeerPrepSession"
    private const val KEY_USER = "user_data"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUser(context: Context, user: User) {
        try {
            val json = gson.toJson(user)
            getPrefs(context).edit().putString(KEY_USER, json).apply()
            Log.d(TAG, "User saved successfully: ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user", e)
        }
    }

    fun getCurrentUser(context: Context): User? {
        val json = getPrefs(context).getString(KEY_USER, null)
        return if (json != null) {
            try {
                gson.fromJson(json, User::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing user JSON", e)
                null
            }
        } else null
    }

    fun saveTokens(context: Context, accessToken: String?, refreshToken: String?) {
        val editor = getPrefs(context).edit()
        accessToken?.let { editor.putString(KEY_ACCESS_TOKEN, it) }
        refreshToken?.let { editor.putString(KEY_REFRESH_TOKEN, it) }
        editor.apply()
    }

    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    fun clearCurrentUser(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun clear(context: Context) {
        clearCurrentUser(context)
    }
}
