package com.prakash.pnotes

import android.app.Application
import com.prakash.pnotes.di.AppModule
import com.prakash.pnotes.sync.SyncInitializer
import com.prakash.pnotes.sync.SyncSettings
import com.prakash.pnotes.widget.NoteAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class PNotesApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (SyncSettings.isEnabled(this)) {
            try {
                SyncInitializer.enqueue(this)
            } catch (e: Exception) {
                // WorkManager is unavailable; sync stays disabled until next launch.
            }
        }
        observeNotesForWidget()
    }

    /**
     * Keeps home-screen widgets in sync with the database while the app process
     * is alive. All note changes originate in the app, so this covers updates.
     */
    private fun observeNotesForWidget() {
        appScope.launch {
            try {
                AppModule.getRepository(this@PNotesApp)
                    .getActiveNotes()
                    .distinctUntilChanged()
                    .collect { NoteAppWidgetProvider.refresh(this@PNotesApp) }
            } catch (e: Exception) {
                // Widget refresh is best-effort; never crash the app for it.
            }
        }
    }
}
