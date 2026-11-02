package com.prakash.pnotes.ui.settings

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pnotes.auth.FirebaseAuthRepository
import com.prakash.pnotes.data.cloud.FirestoreSyncRepository
import com.prakash.pnotes.data.repository.NoteRepository
import com.prakash.pnotes.domain.model.AuthState
import com.prakash.pnotes.domain.model.GoogleSignInResult
import com.prakash.pnotes.domain.model.SyncResult
import com.prakash.pnotes.domain.repository.AuthRepository
import com.prakash.pnotes.sync.SyncInitializer
import com.prakash.pnotes.sync.SyncSettings
import com.prakash.pnotes.widget.NoteAppWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val noteRepository: NoteRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(SyncSettings.PREFS, Context.MODE_PRIVATE)
    private val authRepository: AuthRepository = FirebaseAuthRepository()

    private val _syncEnabled = MutableStateFlow(SyncSettings.isEnabled(application))
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _widgetMessage = MutableStateFlow<String?>(null)
    val widgetMessage: StateFlow<String?> = _widgetMessage.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.authState

    val isFirebaseAvailable: Boolean
        get() = authRepository.isAvailable

    fun toggleSync(enabled: Boolean) {
        _syncEnabled.value = enabled
        prefs.edit().putBoolean(SyncSettings.KEY_SYNC_ENABLED, enabled).apply()
        if (enabled) {
            SyncInitializer.enqueue(getApplication<Application>())
        } else {
            SyncInitializer.cancel(getApplication<Application>())
        }
    }

    /** Runs a one-off sync so the user does not have to wait for the periodic worker. */
    fun syncNow() {
        if (_syncing.value) return
        val userId = authRepository.currentUserId
        if (userId == null) {
            _syncMessage.value = "Sign in before syncing."
            return
        }
        viewModelScope.launch {
            _syncing.value = true
            _syncMessage.value = null
            val result = try {
                FirestoreSyncRepository(userId, noteRepository).sync()
            } catch (e: Exception) {
                SyncResult(success = false, message = e.message ?: "Sync failed")
            }
            _syncMessage.value = result.message
            _syncing.value = false
        }
    }

    /** Asks the launcher to place the notes widget on the home screen. */
    fun requestWidgetPin() {
        val context = getApplication<Application>()
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) {
            _widgetMessage.value = "This launcher does not support pinning widgets."
            return
        }
        val provider = ComponentName(context, NoteAppWidgetProvider::class.java)
        val requested = try {
            manager.requestPinAppWidget(provider, null, null)
        } catch (e: Exception) {
            false
        }
        _widgetMessage.value = if (requested) {
            "Confirm the widget on your home screen."
        } else {
            "Could not add the widget. Add it from your launcher's widget picker."
        }
    }

    /** Call before opening the Credential Manager account picker. */
    fun onGoogleSignInStarted() {
        _authLoading.value = true
        _authMessage.value = null
    }

    fun onGoogleSignInCancelled() {
        _authLoading.value = false
        _authMessage.value = "Google sign-in cancelled."
    }

    fun onGoogleSignInError(message: String) {
        _authLoading.value = false
        _authMessage.value = message
    }

    /** Exchanges a Google ID token for a Firebase session. */
    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _authLoading.value = true
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is GoogleSignInResult.Success -> {
                    _authMessage.value = when {
                        result.linkedAnonymousAccount ->
                            "Google account linked. Your existing notes stay with this account."
                        result.usedExistingAccount ->
                            "Signed in to your existing Google account. Notes on this device are unchanged."
                        else -> "Signed in with Google" +
                            (result.email?.let { " as $it" } ?: "") + "."
                    }
                }
                is GoogleSignInResult.Failure -> _authMessage.value = result.message
            }
            _authLoading.value = false
        }
    }

    fun signIn() {
        _authMessage.value = null
        authRepository.signInAnonymously()
    }

    fun signOut() {
        _authMessage.value = null
        authRepository.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        authRepository.close()
    }
}
