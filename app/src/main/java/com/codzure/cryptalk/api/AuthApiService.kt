package com.codzure.cryptalk.api

import com.codzure.cryptalk.data.User
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API interface for authentication-related network requests.
 */
interface AuthApiService {
    
    /**
     * Registers a new user with the service.
     * 
     * @param request Registration request body containing user details
     * @return API response with the created user on success
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    /**
     * Logs in an existing user with credentials.
     * 
     * @param request Login request body containing credentials
     * @return API response with the authenticated user and token on success
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}

/**
 * Request body for user registration.
 */
@Serializable
data class RegisterRequest(
    val username: String,       // Phone number
    val fullName: String,
    val email: String? = null,
    val phoneNumber: String,
    val password: String,
    val profileImageBase64: String? = null  // Base64 encoded image data
)

/**
 * Request body for user login.
 */
@Serializable
data class LoginRequest(
    val username: String,       // Phone number
    val password: String
)

/**
 * API response for authentication operations.
 */
@Serializable
data class AuthResponse(
    val user: User,
    val token: String
)
