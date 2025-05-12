package com.codzure.cryptalk.api

import android.content.Context
import android.content.SharedPreferences
import com.codzure.cryptalk.data.LoginRequest
import com.codzure.cryptalk.data.RegisterRequest
import com.codzure.cryptalk.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Implementation of the AuthRepository interface for handling authentication operations.
 */
class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val context: Context
) : AuthRepository(apiService, context) {

    companion object {
        private const val PREFS_NAME = "cryptalk_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_USER_ID = "user_id"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Attempts to log in a user with the provided credentials.
     *
     * @param username User's username (often phone number)
     * @param password User's password
     * @return Result containing the logged-in user on success or error message on failure
     */
    override suspend fun login(username: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val loginRequest = LoginRequest(username = username, password = password)
                val response = authApiService.login(loginRequest)

                if (response.isSuccessful) {
                    val loginResponse = response.body() ?: return@withContext Result.failure(
                        IOException("Empty response body")
                    )

                    // Save auth token and user info to preferences
                    saveAuthToken(loginResponse.token)
                    saveCurrentUser(loginResponse.user)

                    Result.success(loginResponse.user)
                } else {
                    // Handle HTTP error
                    val errorMsg = when (response.code()) {
                        401 -> "Invalid credentials"
                        404 -> "User not found"
                        else -> "Login failed: ${response.message()}"
                    }
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(IOException("Network or server error: ${e.localizedMessage}", e))
            }
        }

    /**
     * Registers a new user with enhanced error handling
     */
    override suspend fun register(
        username: String,
        fullName: String,
        phoneNumber: String,
        email: String?,
        password: String,
        profileImageBase64: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val registerRequest = RegisterRequest(
                username = username,
                fullName = fullName,
                phoneNumber = phoneNumber,
                email = email.toString(),
                password = password,
                profileImageBase64 = profileImageBase64
            )

            val response = authApiService.register(registerRequest)

            if (response.isSuccessful) {
                val registerResponse = response.body() ?: return@withContext Result.failure(
                    IOException("Empty response body")
                )

                // Save auth token and user info to preferences
                saveAuthToken(registerResponse.token)
                saveCurrentUser(registerResponse.user)

                Result.success(registerResponse.user)
            } else {
                // Handle HTTP error
                val errorMsg = when (response.code()) {
                    409 -> "User already exists"
                    400 -> "Invalid registration data"
                    else -> "Registration failed: ${response.message()}"
                }
                Result.failure(IOException(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(IOException("Network or server error: ${e.localizedMessage}", e))
        }
    }

    /**
     * Enhanced logout with result tracking
     *
     * @return Result indicating success or failure of the logout operation
     */
    suspend fun logoutWithResult(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Use the base class implementation for actual logout
            logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save current user information to secure storage
     */
    private fun saveCurrentUser(user: User) {
        val userJson = json.encodeToString(user)
        prefs.edit()
            .putString(KEY_CURRENT_USER, userJson)
            .putString(KEY_USER_ID, user.id)
            .apply()
    }
}
