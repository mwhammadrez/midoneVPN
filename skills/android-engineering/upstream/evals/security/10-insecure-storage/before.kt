package com.example.auth

import android.content.Context

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    // BUG: auth token persisted in plain SharedPreferences - readable by
    // anything with access to the app's data directory on a rooted
    // device or via a backup, with no encryption at rest.
    fun saveToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    fun getToken(): String? = prefs.getString("access_token", null)
}
