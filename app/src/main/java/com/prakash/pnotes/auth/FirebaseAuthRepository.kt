package com.prakash.pnotes.auth

import com.google.firebase.auth.FirebaseAuth
import com.prakash.pnotes.domain.model.AuthState
import com.prakash.pnotes.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirebaseAuthRepository : AuthRepository {

    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        // No default FirebaseApp: google-services.json missing or plugin not applied.
        null
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val isAvailable: Boolean
        get() = auth != null

    override val currentUserId: String?
        get() = auth?.currentUser?.uid

    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _authState.value = if (user != null) {
            AuthState.SignedIn(user.uid)
        } else {
            AuthState.SignedOut
        }
    }

    init {
        try {
            auth?.addAuthStateListener(listener)
        } catch (e: Exception) {
            // Ignore listener registration failures.
        }
    }

    override fun signInAnonymously() {
        val firebaseAuth = auth ?: return
        _authState.value = AuthState.Loading
        try {
            firebaseAuth.signInAnonymously().addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _authState.value = AuthState.SignedOut
                }
            }
        } catch (e: Exception) {
            _authState.value = AuthState.SignedOut
        }
    }

    override fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            // Ignore sign-out errors.
        }
    }

    override fun close() {
        try {
            auth?.removeAuthStateListener(listener)
        } catch (e: Exception) {
            // Ignore listener removal failures.
        }
    }
}
