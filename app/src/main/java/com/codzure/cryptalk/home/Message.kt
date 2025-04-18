package com.codzure.cryptalk.home

data class Message(
    val id: String,
    val sender: String,
    val senderNumber: String,
    val encodedText: String,
    val pinHash: String?,
    val isEncrypted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)