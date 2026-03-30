package com.libetario.peerprep.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleSignInHelper {

    suspend fun triggerSignIn(context: Context): String? {
        val credentialManager = CredentialManager.create(context)

        // Use your WEB Client ID from your Google Cloud console here
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("242891288998-bu2al00p956edtgepklq4f1ntoscitv2.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleIdTokenCredential.idToken // Returns the token for Spring Boot
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Error during sign in: ${e.message}")
            null
        }
    }
}