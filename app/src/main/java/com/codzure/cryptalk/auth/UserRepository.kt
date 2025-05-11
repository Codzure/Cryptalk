package com.codzure.cryptalk.auth

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Repository for handling user authentication and user data operations.
 */
class UserRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Attempts to login a user with the provided email and password.
     * 
     * @param email User's email address
     * @param password User's password
     * @return Result containing user data on success or error message on failure
     */
    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // TODO: Replace with actual authentication implementation
            
            // Simulate network delay
            delay(1500)
            
            // For testing purposes, we're "authenticating" any credentials
            // In a real app, you'd validate against your auth provider
            val userId = UUID.randomUUID().toString()
            val user = User(
                id = userId,
                email = email,
                username = email.substringBefore('@'),
                fullName = ""
            )
            
            // Save the logged-in user
            saveCurrentUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Registers a new user with the provided information.
     * 
     * @param fullName User's full name
     * @param username User's chosen username
     * @param email User's email address
     * @param password User's password
     * @param profileImageUri Optional URI to the user's profile image
     * @return Result containing the registered user data on success or error message on failure
     */
    suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        profileImageUri: Uri? = null
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            // TODO: Replace with actual registration implementation
            
            // Simulate network delay
            delay(2000)
            
            // For testing purposes, we're creating a user locally
            // In a real app, you'd register with your auth provider
            val userId = UUID.randomUUID().toString()
            val user = User(
                id = userId,
                fullName = fullName,
                username = username,
                email = email,
                // In a real implementation, you would upload the image and get a URL
                profileImageUrl = profileImageUri?.toString()
            )
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Logs out the current user.
     */
    fun logout() {
        prefs.edit().remove(KEY_CURRENT_USER).apply()
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
     * Checks if a user is currently logged in.
     * 
     * @return True if a user is logged in, false otherwise
     */
    fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
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
    
    companion object {
        private const val PREFS_NAME = "cryptalk_prefs"
        private const val KEY_CURRENT_USER = "current_user"
    }
}
