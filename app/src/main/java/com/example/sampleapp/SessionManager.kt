package com.example.sampleapp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("PaisaLootsPrefs", Context.MODE_PRIVATE)

    fun saveUserSession(token: String, email: String, name: String, role: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("auth_token", token)
            putString("user_email", email)
            putString("user_name", name)
            putString("user_role", role)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    fun getUserName(): String = prefs.getString("user_name", "User") ?: "User"
    fun getUserRole(): String = prefs.getString("user_role", "user") ?: "user"
    fun getAuthToken(): String? = prefs.getString("auth_token", null)

    fun logout() {
        prefs.edit().clear().apply()
    }
}

