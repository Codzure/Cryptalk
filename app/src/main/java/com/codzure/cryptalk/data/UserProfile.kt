package com.codzure.cryptalk.data

import kotlinx.serialization.SerialName

@Serializable
data class UserProfile(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("last_seen") val lastSeen: Long = 0
)