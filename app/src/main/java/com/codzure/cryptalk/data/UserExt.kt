package com.codzure.cryptalk.data

/**
 * Extension functions for the User class
 */

/**
 * Returns a display-friendly name for the user.
 * Prioritizes full name if available, otherwise falls back to username (phone number).
 */
fun User.displayName(): String {
    return when {
        fullName.isNotBlank() -> fullName
        else -> username
    }
}

/**
 * Returns the user's initials (first letter of first and last name if available)
 * Used for avatar placeholder when no image is available
 */
fun User.initials(): String {
    return if (fullName.isNotBlank()) {
        val parts = fullName.split(" ")
        when {
            parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts.last().firstOrNull() ?: ""}".uppercase()
            parts.size == 1 -> "${parts[0].take(2)}".uppercase()
            else -> username.take(2).uppercase()
        }
    } else {
        username.take(2).uppercase()
    }
}
