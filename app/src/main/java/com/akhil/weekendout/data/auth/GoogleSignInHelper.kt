package com.akhil.weekendout.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.akhil.weekendout.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class SignInResult {
    data class Success(val idToken: String) : SignInResult()
    data object Cancelled : SignInResult()
    data class Failed(val message: String) : SignInResult()
}

@Singleton
class GoogleSignInHelper @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    /**
     * Launches Google's account picker via Credential Manager and returns a Google ID
     * token usable with Firebase's GoogleAuthProvider.linkWithCredential.
     *
     * Must be called with an Activity context (not application) so the system sheet has
     * a host. Pass `this` from your @AndroidEntryPoint Activity / Fragment.
     */
    suspend fun signIn(activity: Activity): SignInResult {
        val webClientId = appContext.getString(R.string.default_web_client_id)
        if (webClientId == "PASTE_WEB_CLIENT_ID_HERE" || webClientId.isBlank()) {
            return SignInResult.Failed(
                "Web Client ID not configured. Set R.string.default_web_client_id in strings.xml."
            )
        }

        val cm = CredentialManager.create(appContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            // Show all eligible Google accounts on the device, not just ones already
            // authorized for this app — first-time users won't have any.
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = cm.getCredential(activity, request)
            val cred = response.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idTokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                SignInResult.Success(idTokenCred.idToken)
            } else {
                SignInResult.Failed("Unexpected credential type: ${cred.javaClass.simpleName}")
            }
        } catch (e: GetCredentialException) {
            // User dismissed the sheet, or no Google accounts available.
            if (e.message?.contains("cancelled", ignoreCase = true) == true ||
                e.message?.contains("dismissed", ignoreCase = true) == true
            ) SignInResult.Cancelled else SignInResult.Failed(e.message ?: e.javaClass.simpleName)
        } catch (e: GoogleIdTokenParsingException) {
            SignInResult.Failed("Failed to parse ID token: ${e.message}")
        }
    }
}
