package com.prakash.pnotes

import com.prakash.pnotes.auth.GoogleSignInPolicy
import com.prakash.pnotes.auth.GoogleSignInStrategy
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleSignInPolicyTest {

    @Test
    fun signedOutSessionUsesPlainSignIn() {
        assertEquals(
            GoogleSignInStrategy.SIGN_IN,
            GoogleSignInPolicy.strategyFor(currentUserId = null, isAnonymous = false)
        )
    }

    @Test
    fun anonymousSessionLinksGoogleToPreserveUserId() {
        assertEquals(
            GoogleSignInStrategy.LINK_ANONYMOUS,
            GoogleSignInPolicy.strategyFor(currentUserId = "anonymous-uid", isAnonymous = true)
        )
    }

    @Test
    fun googleSessionIsNotReplaced() {
        assertEquals(
            GoogleSignInStrategy.ALREADY_SIGNED_IN,
            GoogleSignInPolicy.strategyFor(currentUserId = "google-uid", isAnonymous = false)
        )
    }

    @Test
    fun anonymousFlagWithoutUserIdStillSignsIn() {
        assertEquals(
            GoogleSignInStrategy.SIGN_IN,
            GoogleSignInPolicy.strategyFor(currentUserId = null, isAnonymous = true)
        )
    }
}
