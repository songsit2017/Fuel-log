package com.songsit.fuellogpro.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun signIn(context: Context, webClientId: String): String {
        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()
        val result = CredentialManager.create(context).getCredential(context, request)
        val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        return auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
            .await()
            .user
            ?.uid
            ?: error("Google Login did not return a Firebase UID")
    }

    fun signOut() = auth.signOut()
}

