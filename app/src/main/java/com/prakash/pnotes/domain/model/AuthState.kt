package com.prakash.pnotes.domain.model

sealed class AuthState {
    object SignedOut : AuthState()
    object Loading : AuthState()
    data class SignedIn(val userId: String) : AuthState()
}
