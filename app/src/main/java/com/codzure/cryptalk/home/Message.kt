package com.codzure.cryptalk.home

data class Message(
    val id: String,
    val sender: String,
    val encodedText: String,
    val pinHash: String? = null,
    val isEncrypted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)