package com.prakash.pnotes.sync

import android.content.Context

/** Persisted sync preferences shared by the settings screen and app startup. */
object SyncSettings {
    const val PREFS = "pnotes_settings"
    const val KEY_SYNC_ENABLED = "sync_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SYNC_ENABLED, false)
}
