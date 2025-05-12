package com.codzure.cryptalk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to register a new user.
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val profileImageBase64: String? = null
)

/**
 * Response for authentication operations.
 */
@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

/**
 * Request to log in a user.
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Represents an image.
 */
@Serializable
data class ImageData(
    val url: String
)

/**
 * Request to create a new conversation
 */
@Serializable
data class CreateConversationRequest(
    val participantOneId: String,
    val participantTwoId: String
)

/**
 * Request to send a new message
 */
@Serializable
data class SendMessageRequest(
    val senderId: String,
    val recipientId: String,
    val encodedText: String,
    val pinHash: String? = null
)

/**
 * Request to mark messages as read
 */
@Serializable
data class MarkReadRequest(
    val messageIds: List<String>,
    val readerId: String
)

/**
 * Response for marking messages as read
 */
@Serializable
data class MarkReadResponse(
    val success: Boolean,
    val updatedCount: Int
)

/**
 * Enhanced conversation response that includes participant details
 */
@Serializable
data class ConversationResponse(
    val conversation: Conversation,
    val participant: User,
    val lastMessage: Message? = null
)


/**
 * Simple conversation response with just IDs
 */
@Serializable
data class SimpleConversationResponse(
    val id: String,
    val participants: List<String>, // List of user IDs participating in the conversation
    val lastMessage: Message?, // Optional last message in the conversation
    val timestamp: Long // Timestamp of the last activity in the conversation
)


@Serializable
data class Message(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val conversationId: String, // Added for conversation reference
    val text: String,
    val encodedText: String? = null, // Optional field for encoded text
    val pinHash: String? = null, // Optional field for pin hash
    val timestamp: Long,
    val isRead: Boolean = false, // Optional field for read status
    val isDelivered: Boolean = false // Optional field for delivery status
)


@Serializable
data class Conversation(
    @SerialName("id") val id: String = "",
    @SerialName("participant_one_id") val participantOneId: String,
    @SerialName("participant_two_id") val participantTwoId: String,
    @SerialName("last_message_id") val lastMessageId: String? = null,
    @SerialName("last_message_time") val lastMessageTime: Long = 0L,
    @SerialName("unread_one") val unreadOne: Int = 0,
    @SerialName("unread_two") val unreadTwo: Int = 0
)


@Serializable
data class User(
    @SerialName("id") val id: String = "",
    @SerialName("username") val username: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)
