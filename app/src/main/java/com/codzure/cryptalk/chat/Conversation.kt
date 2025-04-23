package com.codzure.cryptalk.chat

data class Conversation(
    val userId: String,
    val userName: String,
    val lastMessage: String,
    val timestamp: Long,
    val isEncrypted: Boolean
)