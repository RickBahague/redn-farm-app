package com.redn.farm.data.local.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun createSession(username: String, role: String) {
        prefs.edit {
            putString(KEY_USERNAME, username)
            putString(KEY_ROLE, role)
            putLong(KEY_LOGIN_TIME, Instant.now().toEpochMilli())
            putBoolean(KEY_IS_LOGGED_IN, true)
        }
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun isAdmin(): Boolean =
        getRole()?.equals("ADMIN", ignoreCase = true) == true

    fun endSession() {
        prefs.edit {
            clear()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    companion object {
        private const val PREF_NAME = "farm_session"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
} 