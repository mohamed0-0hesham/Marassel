package com.hesham0_0.marassel.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject

class GoogleSignInHelper @Inject constructor() {

    companion object {
        const val WEB_CLIENT_ID = "62921054375-5lktptohmu6ska7o3i9hvl3755ssq7h2.apps.googleusercontent.com"
    }

    /**
     * Launches the Credential Manager bottom sheet and returns the
     * Google ID token on success.
     *
     * @param context Activity context required by Credential Manager
     * @return [Result.success(idToken)] on successful selection
     *         [Result.failure(CancellationException)] if user dismisses
     *         [Result.failure] on any other error
     */
    suspend fun getGoogleIdToken(context: Context): Result<String> = runCatching {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Show all accounts, not just previously used
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)           // Don't auto-select — show picker always
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response: GetCredentialResponse = credentialManager.getCredential(
            request = request,
            context = context,
        )

        extractIdToken(response)
    }

    /**
     * Extracts the Google ID token from a [GetCredentialResponse].
     * Throws [IllegalStateException] if the credential type is unexpected.
     */
    private fun extractIdToken(response: GetCredentialResponse): String {
        val credential = response.credential
        return when {
            credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                GoogleIdTokenCredential
                    .createFrom(credential.data)
                    .idToken
            }
            else -> error(
                "Unexpected credential type: ${credential::class.simpleName}. " +
                        "Expected GoogleIdTokenCredential."
            )
        }
    }
}