package com.example.calculaparajugar.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Exporta configuración y progreso. El PIN se excluye deliberadamente. */
object BackupManager {
    private val preferenceFiles = listOf(
        "brainwall_prefs",
        "brainwall_progress",
        "brainwall_lessons",
        "brainwall_adaptive"
    )

    fun export(context: Context): String {
        val root = JSONObject().put("version", 1)
        val preferences = JSONObject()
        preferenceFiles.forEach { file ->
            val values = JSONObject()
            context.getSharedPreferences(file, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                values.put(key, if (value is Set<*>) JSONArray(value.toList()) else value)
            }
            preferences.put(file, values)
        }
        return root.put("preferences", preferences).toString(2)
    }

    fun restore(context: Context, json: String) {
        val root = JSONObject(json)
        require(root.optInt("version") == 1) { "Versión de respaldo no compatible" }
        val preferences = root.getJSONObject("preferences")
        preferenceFiles.forEach { file ->
            if (!preferences.has(file)) return@forEach
            val values = preferences.getJSONObject(file)
            val editor = context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear()
            values.keys().forEach { key ->
                when (val value = values.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                    is JSONArray -> editor.putStringSet(
                        key,
                        (0 until value.length()).map { value.getString(it) }.toSet()
                    )
                }
            }
            check(editor.commit()) { "No se pudo restaurar $file" }
        }
    }
}
