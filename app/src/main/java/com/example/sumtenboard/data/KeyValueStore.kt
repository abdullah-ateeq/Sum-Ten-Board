package com.example.sumtenboard.data

import android.content.SharedPreferences
import androidx.core.content.edit

/** Minimal key-value surface so settings and statistics logic can be unit tested. */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getString(key: String, default: String): String
    fun edit(block: Editor.() -> Unit)
    fun remove(keys: Collection<String>)

    interface Editor {
        fun putBoolean(key: String, value: Boolean)
        fun putInt(key: String, value: Int)
        fun putString(key: String, value: String)
    }
}

class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun getString(key: String, default: String) = prefs.getString(key, default) ?: default

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        prefs.edit {
            val editor = this
            object : KeyValueStore.Editor {
                override fun putBoolean(key: String, value: Boolean) { editor.putBoolean(key, value) }
                override fun putInt(key: String, value: Int) { editor.putInt(key, value) }
                override fun putString(key: String, value: String) { editor.putString(key, value) }
            }.block()
        }
    }

    override fun remove(keys: Collection<String>) {
        prefs.edit { keys.forEach { remove(it) } }
    }
}
