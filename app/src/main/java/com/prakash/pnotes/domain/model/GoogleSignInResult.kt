package com.prakash.pnotes.domain.model

/**
 * Result of exchanging a Google ID token for a Firebase session.
 *
 * When the device already has an anonymous Firebase session, the repository first
 * tries to link the Google credential to it. Linking keeps the same user id, so
 * data already associated with that account is preserved.
 */
sealed class GoogleSignInResult {

    data class Success(
        val email: String?,
        /** True when the Google credential was linked to the existing anonymous account. */
        val linkedAnonymousAccount: Boolean = false,
        /** True when the Google account already existed and the anonymous account could not be linked. */
        val usedExistingAccount: Boolean = false
    ) : GoogleSignInResult()

    data class Failure(val message: String) : GoogleSignInResult()
}
