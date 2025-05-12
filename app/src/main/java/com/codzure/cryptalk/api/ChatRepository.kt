package com.codzure.cryptalk.api

import android.content.Context
import com.codzure.cryptalk.data.Conversation
import com.codzure.cryptalk.data.Message
import com.codzure.cryptalk.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository for managing chat-related operations through the API
 */
class ChatRepository(
    private val chatApiService: ChatApiService,
    private val authRepository: AuthRepository,
    private val context: Context
) {
    // Active conversations cache
    private val _conversations = MutableStateFlow<List<ConversationResponse>>(emptyList())
    val conversations: Flow<List<ConversationResponse>> = _conversations.asStateFlow()
    
    // Active messages cache for the current conversation
    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    
    /**
     * Fetches all conversations for the current user
     * 
     * @return Result containing the list of conversations on success or error message on failure
     */
    suspend fun getConversations(): Result<List<ConversationResponse>> = withContext(Dispatchers.IO) {
        try {
            val currentUser = authRepository.getCurrentUser() ?: return@withContext Result.failure(
                Exception("User not logged in")
            )
            
            val response = chatApiService.getConversations(currentUser.id)
            
            if (response.isSuccessful) {
                val conversations = response.body() ?: emptyList()
                _conversations.value = conversations
                return@withContext Result.success(conversations)
            } else {
                return@withContext Result.failure(
                    Exception("Failed to fetch conversations: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: IOException) {
            return@withContext Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Creates a new conversation with another user
     * 
     * @param otherUserId ID of the user to start conversation with
     * @return Result containing the created conversation on success or error message on failure
     */
    suspend fun createConversation(otherUserId: String): Result<ConversationResponse> = withContext(Dispatchers.IO) {
        try {
            val currentUser = authRepository.getCurrentUser() ?: return@withContext Result.failure(
                Exception("User not logged in")
            )
            
            val request = CreateConversationRequest(
                participantOneId = currentUser.id,
                participantTwoId = otherUserId
            )
            
            val response = chatApiService.createConversation(request)
            
            if (response.isSuccessful) {
                val conversation = response.body() ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )
                
                // Update the conversations cache
                _conversations.value = _conversations.value + conversation
                
                return@withContext Result.success(conversation)
            } else {
                return@withContext Result.failure(
                    Exception("Failed to create conversation: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: IOException) {
            return@withContext Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Gets messages for a specific conversation
     * 
     * @param conversationId ID of the conversation
     * @param refresh Whether to force a refresh from the server
     * @return Result containing the list of messages on success or error message on failure
     */
    suspend fun getMessages(conversationId: String, refresh: Boolean = false): Result<List<Message>> = 
        withContext(Dispatchers.IO) {
            try {
                // Return cached messages if available and refresh not requested
                if (!refresh && _messages.value.containsKey(conversationId)) {
                    return@withContext Result.success(_messages.value[conversationId] ?: emptyList())
                }
                
                val response = chatApiService.getMessages(conversationId)
                
                if (response.isSuccessful) {
                    val messages = response.body() ?: emptyList()
                    
                    // Update the messages cache
                    _messages.value = _messages.value.toMutableMap().apply {
                        put(conversationId, messages)
                    }
                    
                    return@withContext Result.success(messages)
                } else {
                    return@withContext Result.failure(
                        Exception("Failed to fetch messages: ${response.code()} ${response.message()}")
                    )
                }
            } catch (e: IOException) {
                return@withContext Result.failure(Exception("Network error. Please check your connection."))
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    
    /**
     * Sends a new message in a conversation
     * 
     * @param conversationId ID of the conversation
     * @param recipientId ID of the message recipient
     * @param text Plain text content of the message
     * @param pinHash Optional pin hash for encrypted messages
     * @return Result containing the sent message on success or error message on failure
     */
    suspend fun sendMessage(
        conversationId: String,
        recipientId: String,
        text: String,
        pinHash: String? = null
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val currentUser = authRepository.getCurrentUser() ?: return@withContext Result.failure(
                Exception("User not logged in")
            )
            
            // Here we would normally encrypt the message
            // For now, we'll just use the plain text as encodedText
            val encodedText = text
            
            val request = SendMessageRequest(
                senderId = currentUser.id,
                recipientId = recipientId,
                encodedText = encodedText,
                pinHash = pinHash
            )
            
            val response = chatApiService.sendMessage(conversationId, request)
            
            if (response.isSuccessful) {
                val message = response.body() ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )
                
                // Update the messages cache
                val currentMessages = _messages.value[conversationId] ?: emptyList()
                _messages.value = _messages.value.toMutableMap().apply {
                    put(conversationId, currentMessages + message)
                }
                
                return@withContext Result.success(message)
            } else {
                return@withContext Result.failure(
                    Exception("Failed to send message: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: IOException) {
            return@withContext Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Marks messages as read
     * 
     * @param conversationId ID of the conversation
     * @param messageIds List of message IDs to mark as read
     * @return Result indicating success or failure
     */
    suspend fun markMessagesAsRead(
        conversationId: String,
        messageIds: List<String>
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentUser = authRepository.getCurrentUser() ?: return@withContext Result.failure(
                Exception("User not logged in")
            )
            
            val request = MarkReadRequest(
                messageIds = messageIds,
                readerId = currentUser.id
            )
            
            val response = chatApiService.markMessagesAsRead(conversationId, request)
            
            if (response.isSuccessful) {
                val result = response.body()?.success ?: false
                
                // Update the messages cache to mark these messages as read
                if (result && _messages.value.containsKey(conversationId)) {
                    val updatedMessages = _messages.value[conversationId]?.map { message ->
                        if (messageIds.contains(message.id)) {
                            message.copy(isRead = true)
                        } else {
                            message
                        }
                    } ?: emptyList()
                    
                    _messages.value = _messages.value.toMutableMap().apply {
                        put(conversationId, updatedMessages)
                    }
                }
                
                return@withContext Result.success(result)
            } else {
                return@withContext Result.failure(
                    Exception("Failed to mark messages as read: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: IOException) {
            return@withContext Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Searches for users to start a conversation with
     * 
     * @param query Search query (name, phone number, etc.)
     * @return Result containing matching users on success or error message on failure
     */
    suspend fun searchUsers(query: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val currentUser = authRepository.getCurrentUser()
            val username = currentUser?.username ?: currentUser?.phoneNumber
            
            val response = chatApiService.searchUsers(query, username)
            
            if (response.isSuccessful) {
                val users = response.body() ?: emptyList()
                return@withContext Result.success(users)
            } else {
                return@withContext Result.failure(
                    Exception("Failed to search users: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: IOException) {
            return@withContext Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Gets the flow of messages for a specific conversation
     * 
     * @param conversationId ID of the conversation
     * @return Flow of messages for the conversation
     */
    fun getMessagesFlow(conversationId: String): Flow<List<Message>> = 
        MutableStateFlow(_messages.value[conversationId] ?: emptyList()).asStateFlow()
    
    /**
     * Clears cached messages for a conversation
     * 
     * @param conversationId ID of the conversation to clear cache for, or null to clear all
     */
    fun clearMessageCache(conversationId: String? = null) {
        if (conversationId != null) {
            _messages.value = _messages.value.toMutableMap().apply {
                remove(conversationId)
            }
        } else {
            _messages.value = emptyMap()
        }
    }
    
    /**
     * Gets the currently logged-in user from the auth repository
     * 
     * @return The current user or null if no user is logged in
     */
    fun getCurrentUser(): User? {
        return authRepository.getCurrentUser()
    }
}
