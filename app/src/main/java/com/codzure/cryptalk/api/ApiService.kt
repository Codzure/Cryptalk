package com.codzure.cryptalk.api

import com.codzure.cryptalk.data.AuthResponse
import com.codzure.cryptalk.data.ConversationResponse
import com.codzure.cryptalk.data.CreateConversationRequest
import com.codzure.cryptalk.data.ImageData
import com.codzure.cryptalk.data.LoginRequest
import com.codzure.cryptalk.data.MarkReadRequest
import com.codzure.cryptalk.data.MarkReadResponse
import com.codzure.cryptalk.data.Message
import com.codzure.cryptalk.data.RegisterRequest
import com.codzure.cryptalk.data.SendMessageRequest
import com.codzure.cryptalk.data.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Combined API interface for authentication and chat-related network requests.
 */
interface ApiService {

    // Authentication APIs
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Any>

    // User Profile APIs
    @PUT("users/profile-image")
    suspend fun updateProfileImage(
        @Header("Authorization") token: String,
        @Body image: ImageData
    ): Response<Any>

    @DELETE("users/profile-image")
    suspend fun removeProfileImage(@Header("Authorization") token: String): Response<Any>

    @GET("users/search")
    suspend fun searchUsers(@Query("query") query: String): Response<List<User>>

    // Conversation APIs
    @GET("conversations")
    suspend fun getConversations(@Query("userId") userId: String): Response<List<ConversationResponse>>

    @POST("conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest): Response<ConversationResponse>

    @GET("conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("lastMessageId") lastMessageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<Message>>

    @POST("conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<Message>

    @POST("conversations/{conversationId}/read")
    suspend fun markMessagesAsRead(
        @Path("conversationId") conversationId: String,
        @Body request: MarkReadRequest
    ): Response<MarkReadResponse>
}