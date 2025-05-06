package com.codzure.cryptalk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id") val id: String = "",
    @SerialName("username") val username: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)


/*create table users (
id uuid primary key default gen_random_uuid(),
username text unique,
full_name text,
email text unique,
phone_number text unique not null, -- ✅ Required for login/OTP
avatar_url text,
created_at timestamp with time zone default now(),
updated_at timestamp with time zone default now()
);*/
