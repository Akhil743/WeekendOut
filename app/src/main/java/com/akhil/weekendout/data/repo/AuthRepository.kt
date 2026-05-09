package com.akhil.weekendout.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

data class AuthUser(val uid: String, val isAnonymous: Boolean, val email: String?)

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val appCheck: FirebaseAppCheck
) {
    fun observeUser(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            val u = fa.currentUser
            trySend(u?.let { AuthUser(it.uid, it.isAnonymous, it.email) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun ensureSignedIn(): AuthUser {
        val current = auth.currentUser
        if (current != null) return AuthUser(current.uid, current.isAnonymous, current.email)
        val res = auth.signInAnonymously().await()
        val u = res.user!!
        return AuthUser(u.uid, u.isAnonymous, u.email)
    }

    /** Upgrades the anonymous user to a Google-linked one. ID token comes from CredentialManager. */
    suspend fun linkGoogle(googleIdToken: String): AuthUser {
        val cred = GoogleAuthProvider.getCredential(googleIdToken, null)
        val current = auth.currentUser ?: throw IllegalStateException("No user to link")
        val res = if (current.isAnonymous) current.linkWithCredential(cred).await()
        else auth.signInWithCredential(cred).await()
        val u = res.user!!
        return AuthUser(u.uid, u.isAnonymous, u.email)
    }

    /** Used by the Worker call to prove the request is from a real APK. */
    suspend fun appCheckToken(): String =
        appCheck.getAppCheckToken(false).await().token

    /** Synchronous read of current auth state. True if no user OR user is anonymous. */
    fun currentUserIsAnonymous(): Boolean {
        val u = auth.currentUser ?: return true
        return u.isAnonymous
    }
}
