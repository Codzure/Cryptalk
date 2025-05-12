package com.codzure.cryptalk.api

import android.content.Context
import android.content.SharedPreferences
import com.codzure.cryptalk.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

/**
 * Repository for handling authentication operations through the API.
 */
open class AuthRepository(
    private val authApiService: AuthApiService,
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "cryptalk_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CURRENT_USER = "current_user"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Registers a new user with the provided information.
     *
     * @param username User's phone number (used as username)
     * @param fullName User's full name
     * @param phoneNumber User's phone number
     * @param email User's email (optional)
     * @param password User's password
     * @return Result containing the registered user on success or error message on failure
     */
    open suspend fun register(
        username: String,
        fullName: String,
        phoneNumber: String,
        email: String?,
        password: String,
        profileImageBase64: String? = null
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val registerRequest = RegisterRequest(
                username = username,
                fullName = fullName,
                phoneNumber = phoneNumber,
                email = email,
                password = password,
                profileImageBase64 = profileImageBase64
            )

            val response = authApiService.register(registerRequest)
            return@withContext handleAuthResponse(response)
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs in a user with the provided credentials.
     *
     * @param username User's phone number (used as username)
     * @param password User's password
     * @return Result containing the authenticated user on success or error message on failure
     */
    open suspend fun login(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val loginRequest = LoginRequest(username, password)
            val response = authApiService.login(loginRequest)
            return@withContext handleAuthResponse(response)
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs out the current user by clearing saved credentials.
     */
    fun logout() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_CURRENT_USER)
            .apply()
    }

    /**
     * Gets the currently logged-in user, if any.
     *
     * @return The current user or null if no user is logged in
     */
    fun getCurrentUser(): User? {
        val userJson = prefs.getString(KEY_CURRENT_USER, null) ?: return null
        return try {
            json.decodeFromString<User>(userJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets the authentication token for the current session.
     *
     * @return The auth token or null if no user is logged in
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Handles API responses for authentication operations.
     *
     * @param response The API response
     * @return Result with the user on success or an error on failure
     */
    private fun handleAuthResponse(response: Response<AuthResponse>): Result<User> {
        if (response.isSuccessful) {
            val authResponse = response.body()
            if (authResponse != null) {
                // Save auth token and user info
                saveAuthToken(authResponse.token)
                saveCurrentUser(authResponse.user)
                return Result.success(authResponse.user)
            }
        }
        val errorMessage = response.errorBody()?.string() ?: "Unknown error occurred"
        return Result.failure(Exception(errorMessage))
    }

    /**
     * Saves the authentication token to persistent storage.
     *
     * @param token The auth token to save
     */
    internal fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    /**
     * Saves the current user to persistent storage.
     *
     * @param user The user to save
     */
    private fun saveCurrentUser(user: User) {
        val userJson = json.encodeToString(user)
        prefs.edit().putString(KEY_CURRENT_USER, userJson).apply()
    }
}
