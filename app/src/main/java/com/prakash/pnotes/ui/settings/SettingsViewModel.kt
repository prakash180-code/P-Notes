package com.prakash.pnotes.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pnotes.auth.FirebaseAuthRepository
import com.prakash.pnotes.data.cloud.FirestoreSyncRepository
import com.prakash.pnotes.data.repository.NoteRepository
import com.prakash.pnotes.domain.model.AuthState
import com.prakash.pnotes.domain.model.SyncResult
import com.prakash.pnotes.domain.repository.AuthRepository
import com.prakash.pnotes.sync.SyncInitializer
import com.prakash.pnotes.sync.SyncSettings
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

    fun signIn() {
        authRepository.signInAnonymously()
    }

    fun signOut() {
        authRepository.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        authRepository.close()
    }
}
