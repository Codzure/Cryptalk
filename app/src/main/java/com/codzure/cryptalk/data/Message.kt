package com.codzure.cryptalk.data

data class Message(
    val id: String,
    val sender: String,
    val senderNumber: String,
    val encodedText: String,
    val pinHash: String?,
    val isEncrypted: Boolean, // remove this. If hash is nil, then message is not encrypted, else encrypted.
    val timestamp: Long = System.currentTimeMillis()
)


/*@Serializable
data class Message(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("encoded_text") val encodedText: String,
    @SerialName("pin_hash") val pinHash: String? = null,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerialName("read") val read: Boolean = false
)*/