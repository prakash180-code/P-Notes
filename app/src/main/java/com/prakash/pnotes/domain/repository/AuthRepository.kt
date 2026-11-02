package com.prakash.pnotes.domain.repository

import com.prakash.pnotes.domain.model.AuthState
import com.prakash.pnotes.domain.model.GoogleSignInResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>

    /** False when Firebase is not configured (e.g. missing google-services.json). */
    val isAvailable: Boolean

    val currentUserId: String?

    fun signInAnonymously()

    /**
     * Signs in with a Google ID token.
     *
     * If the current session is anonymous, the credential is linked to it first so
     * the existing user id (and any data attached to it) is preserved. If the
     * Google account already belongs to another Firebase user, that account is
     * signed in to instead and the anonymous account is left untouched.
     */
    suspend fun signInWithGoogle(idToken: String): GoogleSignInResult

    fun signOut()

    /** Releases listeners held by this instance. */
    fun close() {}
}
