package com.libetario.peerprep.auth

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val pref: SharedPreferences = context.getSharedPreferences(
        "peerprep_session",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_AUTH_TOKEN = "authToken"
    }

    fun saveUserSession(email: String, name: String, token: String = "") {
        pref.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserEmail(): String? {
        return pref.getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return pref.getString(KEY_USER_NAME, null)
    }

    fun getAuthToken(): String? {
        return pref.getString(KEY_AUTH_TOKEN, null)
    }

    fun logout() {
        pref.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putString(KEY_USER_EMAIL, null)
            .putString(KEY_USER_NAME, null)
            .putString(KEY_AUTH_TOKEN, null)
            .apply()
    }

    fun clearAll() {
        pref.edit().clear().apply()
    }
}
