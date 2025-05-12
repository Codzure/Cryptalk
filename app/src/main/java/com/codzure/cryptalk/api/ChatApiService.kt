package com.codzure.cryptalk.api

import com.codzure.cryptalk.data.Conversation
import com.codzure.cryptalk.data.Message
import com.codzure.cryptalk.data.User
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

/**
 * API interface for chat-related operations
 */
interface ChatApiService {
    /**
     * Gets all conversations for the current user
     * 
     * @param userId The ID of the current user
     * @return List of conversations
     */
    @GET("conversations")
    suspend fun getConversations(@Query("userId") userId: String): Response<List<ConversationResponse>>
    
    /**
     * Creates a new conversation between two users
     * 
     * @param request Request containing the conversation participants
     * @return The created conversation
     */
    @POST("conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest): Response<ConversationResponse>
    
    /**
     * Gets messages for a specific conversation
     * 
     * @param conversationId The ID of the conversation
     * @param lastMessageId Optional ID of the last message received for pagination
     * @param limit Maximum number of messages to return
     * @return List of messages
     */
    @GET("conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("lastMessageId") lastMessageId: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<Message>>
    
    /**
     * Sends a new message in a conversation
     * 
     * @param conversationId The ID of the conversation
     * @param request Request containing the message details
     * @return The sent message
     */
    @POST("conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<Message>
    
    /**
     * Marks messages as read
     * 
     * @param conversationId The ID of the conversation
     * @param request Request containing the message IDs to mark as read
     * @return Success response
     */
    @POST("conversations/{conversationId}/read")
    suspend fun markMessagesAsRead(
        @Path("conversationId") conversationId: String,
        @Body request: MarkReadRequest
    ): Response<MarkReadResponse>
    
    /**
     * Searches for users to start a conversation with
     * 
     * @param query Search query (name, phone number, etc.)
     * @param username Current user's username (to exclude from results)
     * @return List of matching users
     */
    @GET("users/search")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("username") username: String? = null
    ): Response<List<User>>
}

/**
 * Request to create a new conversation
 */
@Serializable
data class CreateConversationRequest(
    val participantOneId: String,
    val participantTwoId: String
)

/**
 * Request to send a new message
 */
@Serializable
data class SendMessageRequest(
    val senderId: String,
    val recipientId: String,
    val encodedText: String,
    val pinHash: String? = null
)

/**
 * Request to mark messages as read
 */
@Serializable
data class MarkReadRequest(
    val messageIds: List<String>,
    val readerId: String
)

/**
 * Response for marking messages as read
 */
@Serializable
data class MarkReadResponse(
    val success: Boolean,
    val updatedCount: Int
)

/**
 * Enhanced conversation response that includes participant details
 */
@Serializable
data class ConversationResponse(
    val conversation: Conversation,
    val participant: User,
    val lastMessage: Message? = null
)
