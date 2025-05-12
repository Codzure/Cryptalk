package com.codzure.cryptalk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID


@Serializable
data class Message(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("encoded_text") val encodedText: String,
    @SerialName("pin_hash") val pinHash: String? = null,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("is_delivered") val isDelivered: Boolean = false
)


/*
create table messages (
id uuid primary key default gen_random_uuid(),
conversation_id uuid references conversations(id) on delete cascade,
sender_id uuid references users(id) on delete cascade,
recipient_id uuid references users(id) on delete cascade,
encoded_text text not null,
pin_hash text,
timestamp bigint default (extract(epoch from now()) * 1000)::bigint,
is_read boolean default false,
is_delivered boolean default false
);*/
