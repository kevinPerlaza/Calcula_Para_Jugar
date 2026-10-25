package com.example.calculaparajugar.util

import android.content.Context
import java.security.MessageDigest

/**
 * Gestiona el PIN del padre para proteger la configuración
 * y la desinstalación de la app.
 */
object PinManager {

    private const val PREFS_NAME = "pin_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_INITIAL_PROMPT_DISMISSED = "initial_prompt_dismissed"

    fun hasPin(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun setPin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PIN_HASH, hashPin(pin))
            .putBoolean(KEY_INITIAL_PROMPT_DISMISSED, false)
            .apply()
    }

    fun shouldShowInitialPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !hasPin(context) && !prefs.getBoolean(KEY_INITIAL_PROMPT_DISMISSED, false)
    }

    fun dismissInitialPrompt(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INITIAL_PROMPT_DISMISSED, true)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return storedHash == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
