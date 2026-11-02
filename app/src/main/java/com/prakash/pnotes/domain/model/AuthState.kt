package com.prakash.pnotes.domain.model

sealed class AuthState {
    object SignedOut : AuthState()
    object Loading : AuthState()

    data class SignedIn(
        val userId: String,
        val email: String? = null,
        val isAnonymous: Boolean = false
    ) : AuthState()
}
