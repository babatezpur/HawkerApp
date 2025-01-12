package com.hawkerapp.app.store

import android.content.Context

object SessionManager {
    private const val PREF_NAME = "HawkerAppSession"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_HAWKER_ID = "hawker_id"

    fun saveSession(context: Context, hawkerId: String, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_HAWKER_ID, hawkerId)
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun getHawkerId(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val id = prefs.getInt(KEY_HAWKER_ID, -1)
        return if (id == -1) null else id
    }

    fun getAuthToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getAuthToken(context) != null && getHawkerId(context) != null
    }
}