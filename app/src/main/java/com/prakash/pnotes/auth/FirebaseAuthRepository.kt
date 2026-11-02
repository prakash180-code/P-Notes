package com.prakash.pnotes.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.prakash.pnotes.domain.model.AuthState
import com.prakash.pnotes.domain.model.GoogleSignInResult
import com.prakash.pnotes.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

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
            AuthState.SignedIn(
                userId = user.uid,
                email = user.email,
                isAnonymous = user.isAnonymous
            )
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

    override suspend fun signInWithGoogle(idToken: String): GoogleSignInResult {
        val firebaseAuth = auth ?: return GoogleSignInResult.Failure(
            "Firebase is not configured. Add google-services.json and enable the Google Services plugin."
        )

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val currentUser = firebaseAuth.currentUser

        return try {
            when (
                GoogleSignInPolicy.strategyFor(
                    currentUserId = currentUser?.uid,
                    isAnonymous = currentUser?.isAnonymous == true
                )
            ) {
                GoogleSignInStrategy.ALREADY_SIGNED_IN -> GoogleSignInResult.Failure(
                    "You are already signed in with Google. Sign out first to use a different account."
                )

                GoogleSignInStrategy.LINK_ANONYMOUS -> linkAnonymousAccount(
                    firebaseAuth = firebaseAuth,
                    currentUser = requireNotNull(currentUser),
                    credential = credential
                )

                GoogleSignInStrategy.SIGN_IN -> {
                    val result = firebaseAuth.signInWithCredential(credential).await()
                    GoogleSignInResult.Success(
                        email = result.user?.email,
                        linkedAnonymousAccount = false
                    )
                }
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            GoogleSignInResult.Failure(
                "This Google account is already linked to another P-Notes account. " +
                    "Your local notes were not changed."
            )
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            GoogleSignInResult.Failure(
                "For security, sign out and sign in again before linking Google. " +
                    "Your local notes were not changed."
            )
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            GoogleSignInResult.Failure("Google sign-in was rejected. Please try again.")
        } catch (e: FirebaseNetworkException) {
            GoogleSignInResult.Failure("Network error. Check your connection and try again.")
        } catch (e: Exception) {
            GoogleSignInResult.Failure(e.message ?: "Google sign-in failed.")
        }
    }

    /**
     * Links Google to the current anonymous user so the user id does not change.
     * If the Google account is already used by another Firebase user, signs in to
     * that existing account instead. The anonymous account is never deleted and
     * the Room database is never touched.
     */
    private suspend fun linkAnonymousAccount(
        firebaseAuth: FirebaseAuth,
        currentUser: FirebaseUser,
        credential: AuthCredential
    ): GoogleSignInResult {
        return try {
            currentUser.linkWithCredential(credential).await()
            GoogleSignInResult.Success(
                email = firebaseAuth.currentUser?.email,
                linkedAnonymousAccount = true
            )
        } catch (e: FirebaseAuthUserCollisionException) {
            val result = firebaseAuth.signInWithCredential(credential).await()
            GoogleSignInResult.Success(
                email = result.user?.email,
                linkedAnonymousAccount = false,
                usedExistingAccount = true
            )
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
