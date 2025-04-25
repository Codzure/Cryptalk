package com.codzure.cryptalk.data

data class Conversation(
    val userId: String,
    val userName: String,
    val lastMessage: String,
    val timestamp: Long,
    val isEncrypted: Boolean
)


/**
 * Chat conversation model to group messages
 */
/*
@Serializable
data class Conversation(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user1_id") val user1Id: String,
    @SerialName("user2_id") val user2Id: String,
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("last_message_encrypted") val lastMessageEncrypted: Boolean = false,
    @SerialName("last_message_time") val lastMessageTime: Long = 0,
    @SerialName("unread_count") val unreadCount: Int = 0
)*/
