package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleAuthAccount(
    val uid: String,
    val displayName: String,
    val email: String
)

object GoogleAuthHelper {
    private const val TAG = "Auth"

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: (GoogleAuthAccount) -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val current = Firebase.auth.currentUser
        if (current != null) {
            onAuthSuccess(
                GoogleAuthAccount(
                    uid = current.uid,
                    displayName = current.displayName.orEmpty(),
                    email = current.email.orEmpty()
                )
            )
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val cred = credentialManager.getCredential(context, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val tokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                    val result = Firebase.auth
                        .signInWithCredential(GoogleAuthProvider.getCredential(tokenCred.idToken, null))
                        .await()
                    val user = result.user
                    if (user != null) {
                        onAuthSuccess(
                            GoogleAuthAccount(
                                uid = user.uid,
                                displayName = user.displayName ?: tokenCred.displayName.orEmpty(),
                                email = user.email ?: tokenCred.id
                            )
                        )
                    } else {
                        onUnauthenticated()
                    }
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun launchGoogleSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: (GoogleAuthAccount) -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("تعذر العثور على إعدادات تسجيل الدخول بواسطة غوغل")
            return
        }
        val activity = context as? Activity
        if (activity == null) {
            onAuthError("تعذر فتح نافذة اختيار حساب غوغل")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val tokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                    val authResult = Firebase.auth
                        .signInWithCredential(GoogleAuthProvider.getCredential(tokenCred.idToken, null))
                        .await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        onAuthSuccess(
                            GoogleAuthAccount(
                                uid = firebaseUser.uid,
                                displayName = firebaseUser.displayName ?: tokenCred.displayName.orEmpty(),
                                email = firebaseUser.email ?: tokenCred.id
                            )
                        )
                    } else {
                        onAuthError("تعذر إتمام المصادقة بحساب غوغل")
                    }
                } else {
                    onAuthError("نوع بيانات الاعتماد غير مدعوم")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "فشل تسجيل الدخول بواسطة غوغل")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onComplete()
                }
            }
        }
    }
}
