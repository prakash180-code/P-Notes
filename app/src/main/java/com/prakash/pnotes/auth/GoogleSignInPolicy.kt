package com.prakash.pnotes.auth

/**
 * What to do with a Google credential, based on the current Firebase session.
 * Kept as a pure function so the account-safety rules can be unit tested.
 */
enum class GoogleSignInStrategy {
    /** A non-anonymous user is already signed in; do not replace the session. */
    ALREADY_SIGNED_IN,

    /** An anonymous user is signed in; link Google to it so its data stays attached. */
    LINK_ANONYMOUS,

    /** No user is signed in; sign in with the Google credential directly. */
    SIGN_IN
}

object GoogleSignInPolicy {

    fun strategyFor(currentUserId: String?, isAnonymous: Boolean): GoogleSignInStrategy = when {
        currentUserId == null -> GoogleSignInStrategy.SIGN_IN
        isAnonymous -> GoogleSignInStrategy.LINK_ANONYMOUS
        else -> GoogleSignInStrategy.ALREADY_SIGNED_IN
    }
}
