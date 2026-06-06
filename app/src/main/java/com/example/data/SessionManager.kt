package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "CollegeAppPrefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_ROLE = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
    }

    fun saveSession(userId: Long, name: String, email: String, role: String, token: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, userId)
            putString(KEY_NAME, name)
            putString(KEY_EMAIL, email)
            putString(KEY_ROLE, role)
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    fun getName(): String {
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    fun getEmail(): String {
        return prefs.getString(KEY_EMAIL, "") ?: ""
    }

    fun getRole(): String {
        return prefs.getString(KEY_ROLE, "") ?: ""
    }

    fun getToken(): String {
        return prefs.getString(KEY_TOKEN, "") ?: ""
    }

    fun clearSession() {
        prefs.edit().apply {
            clear()
            apply()
        }
    }
}
