package com.codzure.cryptalk.models

/**
 * UI representation of a conversation with additional display data
 * This bridges the database model with the UI requirements
 */
data class ConversationUI(
    val id: String,
    val userId: String,
    val userName: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val isEncrypted: Boolean,
    val unreadCount: Int
)
