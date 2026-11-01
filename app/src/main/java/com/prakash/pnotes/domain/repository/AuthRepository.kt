package com.prakash.pnotes.domain.repository

import com.prakash.pnotes.domain.model.AuthState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>

    /** False when Firebase is not configured (e.g. missing google-services.json). */
    val isAvailable: Boolean

    val currentUserId: String?

    fun signInAnonymously()
    fun signOut()

    /** Releases listeners held by this instance. */
    fun close() {}
}
