package com.codzure.cryptalk.extensions

import android.content.ContentValues.TAG
import android.util.Log
import com.codzure.cryptalk.SupabaseManager.client
import com.codzure.cryptalk.data.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Authentication operations
 */
object Auth {
    suspend fun signUp(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = client.auth.signUpEmailPassword(email, password)
            return@withContext Result.success(response.session?.user?.id ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = client.auth.signInEmailPassword(email, password)
            return@withContext Result.success(response.session?.user?.id ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(): Result<String> = withContext(Dispatchers.IO) {
        try {
            client.auth.signInWith(OAuthProvider.GOOGLE)
            val user = client.auth.currentUserOrNull()
            return@withContext if (user != null) {
                Result.success(user.id)
            } else {
                Result.failure(Exception("Failed to get user after Google sign-in"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.auth.signOut()
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun createUserProfile(userId: String, username: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val profile = UserProfile(
                id = userId,
                username = username,
                lastSeen = System.currentTimeMillis()
            )

            client.postgrest["user_profiles"].insert(profile)
            return@withContext Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user profile: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    suspend fun updateLastSeen(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: return@withContext Result.failure(Exception("No user logged in"))

        try {
            client.postgrest["user_profiles"]
                .update({
                    UserProfile::lastSeen setTo System.currentTimeMillis()
                }) {
                    UserProfile::id eq userId
                }

            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last seen: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
}