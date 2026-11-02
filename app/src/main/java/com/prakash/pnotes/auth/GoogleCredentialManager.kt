package com.prakash.pnotes.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException

sealed class GoogleIdTokenResult {
    data class Success(val idToken: String) : GoogleIdTokenResult()
    object Cancelled : GoogleIdTokenResult()
    data class Failure(val message: String) : GoogleIdTokenResult()
}

/**
 * Requests a Google ID token through Android Credential Manager. The returned
 * token is exchanged for a Firebase session by [FirebaseAuthRepository].
 *
 * [context] should be an Activity context so Credential Manager can show its
 * account picker.
 */
class GoogleCredentialManager(private val context: Context) {

    suspend fun requestIdToken(serverClientId: String): GoogleIdTokenResult {
        if (serverClientId.isBlank()) {
            return GoogleIdTokenResult.Failure(
                "Google Sign-In is not configured: default_web_client_id is missing."
            )
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    // Allow any Google account, including first-time sign-ins.
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    // Always show the picker instead of silently choosing an account.
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        return try {
            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleIdTokenResult.Success(googleCredential.idToken)
            } else {
                GoogleIdTokenResult.Failure("Google returned an unsupported credential type.")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleIdTokenResult.Failure(
                "No Google account is available. Add one in Android Settings and try again."
            )
        } catch (e: GetCredentialException) {
            GoogleIdTokenResult.Failure(e.message ?: "Google sign-in failed.")
        } catch (e: GoogleIdTokenParsingException) {
            GoogleIdTokenResult.Failure("Google returned an invalid ID token.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GoogleIdTokenResult.Failure(e.message ?: "Google sign-in failed.")
        }
    }
}
