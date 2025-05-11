package com.codzure.cryptalk.auth

import kotlinx.serialization.Serializable

/**
 * Data class representing a user in the Cryptalk application.
 * This model is used for authentication and user profile management.
 */
@Serializable
data class User(
    val id: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
) {
    /**
     * Returns a display-friendly name for the user.
     * Uses username if available, otherwise falls back to email or a default value.
     */
    fun displayName(): String {
        return when {
            username.isNotBlank() -> username
            email.isNotBlank() -> email.substringBefore('@')
            else -> "Unknown User"
        }
    }
    
    /**
     * Returns the initial letters for the user's avatar (if no profile image is available).
     * Extracts up to 2 initials from the full name, or uses the first letter of the username/email.
     */
    fun initials(): String {
        return when {
            fullName.isNotBlank() -> {
                fullName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().toString() }
                    .uppercase()
            }
            username.isNotBlank() -> username.first().uppercase()
            email.isNotBlank() -> email.first().uppercase()
            else -> "?"
        }
    }
}
