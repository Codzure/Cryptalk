package com.codzure.cryptalk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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


/*
create table conversations (
id uuid primary key default gen_random_uuid(),
participant_one_id uuid references users(id) on delete cascade,
participant_two_id uuid references users(id) on delete cascade,
last_message_id uuid references messages(id) on delete set null,
last_message_time bigint default (extract(epoch from now()) * 1000)::bigint,
unread_one int default 0,
unread_two int default 0,
constraint unique_participants unique (participant_one_id, participant_two_id)
);*/
