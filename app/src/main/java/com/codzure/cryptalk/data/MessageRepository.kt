package com.codzure.cryptalk.data

import com.codzure.cryptalk.extensions.AESAlgorithm
import com.codzure.cryptalk.models.ConversationUI
import com.codzure.cryptalk.utils.DataUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository interface for handling all message-related operations
 */
interface MessageRepository {
    fun getMessages(conversationId: String): Flow<List<Message>>
    fun getAllMessages(): Flow<List<Message>>
    fun getConversations(): Flow<List<Conversation>>
    fun getConversationsUI(): Flow<List<ConversationUI>>
    suspend fun sendMessage(conversationId: String, recipientId: String, text: String, encrypt: Boolean = false, pin: String? = null): Message
    suspend fun decryptMessage(message: Message, pin: String): String
    suspend fun getUserById(userId: String): User?
    suspend fun markConversationAsRead(conversationId: String)
}

/**
 * Implementation of MessageRepository using your actual data models
 */
class MessageRepositoryImpl : MessageRepository {
    // In-memory storage for demo
    private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    private val conversationsFlow = MutableStateFlow<List<Conversation>>(emptyList())
    private val usersFlow = MutableStateFlow<List<User>>(emptyList())
    
    init {
        // Generate dummy data using your data models
        val users = listOf(DataUtils.currentUser) + DataUtils.dummyUsers
        usersFlow.value = users
        
        val conversations = DataUtils.generateDummyConversations()
        conversationsFlow.value = conversations
        
        val messages = DataUtils.generateDummyMessages()
        messagesFlow.value = messages
    }
    
    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return messagesFlow.map { messages ->
            messages.filter { it.conversationId == conversationId }
                .sortedBy { it.timestamp }
        }
    }

    override fun getAllMessages(): Flow<List<Message>> {
        return messagesFlow
    }

    override fun getConversations(): Flow<List<Conversation>> {
        return conversationsFlow
    }
    
    override fun getConversationsUI(): Flow<List<ConversationUI>> {
        return conversationsFlow.map { conversations ->
            conversations.map { conversation ->
                val messages = messagesFlow.value.filter { it.conversationId == conversation.id }
                val lastMessage = messages.maxByOrNull { it.timestamp }
                
                // Determine other user ID (the one who's not the current user)
                val otherUserId = if (conversation.participantOneId == DataUtils.currentUser.id) {
                    conversation.participantTwoId
                } else {
                    conversation.participantOneId
                }
                
                // Get the other user
                val otherUser = usersFlow.value.find { it.id == otherUserId }
                
                // Create the UI model
                ConversationUI(
                    id = conversation.id,
                    userId = otherUserId,
                    userName = otherUser?.fullName ?: "Unknown User",
                    lastMessage = lastMessage?.let {
                        if (it.pinHash != null) "🔒 Encrypted Message" else it.encodedText
                    } ?: "No messages",
                    lastMessageTime = lastMessage?.timestamp ?: conversation.lastMessageTime,
                    isEncrypted = lastMessage?.pinHash != null,
                    unreadCount = if (conversation.participantOneId == DataUtils.currentUser.id) {
                        conversation.unreadOne
                    } else {
                        conversation.unreadTwo
                    }
                )
            }.sortedByDescending { it.lastMessageTime }
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        recipientId: String, 
        text: String, 
        encrypt: Boolean,
        pin: String?
    ): Message {
        // Use AESAlgorithm to encrypt the message if needed
        val messageText = if (encrypt && pin != null) {
            try {
                // Encrypt the message text using AES encryption
                AESAlgorithm.AES.encrypt(text, pin)
            } catch (e: Exception) {
                throw Exception("Failed to encrypt message: ${e.message}")
            }
        } else {
            text
        }
        
        // Create message using your data model
        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = DataUtils.currentUser.id,
            recipientId = recipientId,
            encodedText = messageText,
            pinHash = if (encrypt && pin != null) generatePinHash(pin) else null,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        
        // Add the message to our collection
        val currentMessages = messagesFlow.value.toMutableList()
        currentMessages.add(newMessage)
        messagesFlow.value = currentMessages
        
        // Update the conversation's last message details
        updateConversationLastMessage(conversationId, newMessage.id, newMessage.timestamp)
        
        // Generate auto-reply with slight delay
        delay(800) // Simulate network delay
        generateAutoReply(conversationId, recipientId, text, encrypt, pin)
        
        return newMessage
    }
    
    private fun generateAutoReply(
        conversationId: String,
        senderId: String,
        originalText: String,
        encrypt: Boolean,
        pin: String?
    ) {
        // Create a simple auto-reply
        val replyText = when {
            originalText.contains("hello", ignoreCase = true) -> "Hello there! How can I help you today?"
            originalText.contains("hi", ignoreCase = true) -> "Hi! Nice to hear from you."
            originalText.contains("how are you", ignoreCase = true) -> "I'm doing great, thanks for asking! How about you?"
            originalText.contains("help", ignoreCase = true) -> "I'm here to help. What do you need assistance with?"
            originalText.contains("bye", ignoreCase = true) -> "Goodbye! Talk to you later."
            originalText.contains("?") -> "That's an interesting question. Let me think about it."
            else -> "Thanks for your message. I'll get back to you soon."
        }
        
        // Encrypt the reply if needed
        val encryptedReplyText = if (encrypt && pin != null) {
            try {
                AESAlgorithm.AES.encrypt(replyText, pin)
            } catch (e: Exception) {
                // If encryption fails, use plain text but log the error
                android.util.Log.e("Cryptalk", "Failed to encrypt auto-reply: ${e.message}")
                replyText
            }
        } else {
            replyText
        }
        
        // Create auto-reply message
        val replyMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId, // The recipient becomes the sender for the reply
            recipientId = DataUtils.currentUser.id,
            encodedText = encryptedReplyText,
            pinHash = if (encrypt && pin != null) generatePinHash(pin) else null,
            timestamp = System.currentTimeMillis(),
            isRead = true
        )
        
        // Add reply to messages
        val currentMessages = messagesFlow.value.toMutableList()
        currentMessages.add(replyMessage)
        messagesFlow.value = currentMessages
        
        // Update conversation last message
        updateConversationLastMessage(conversationId, replyMessage.id, replyMessage.timestamp)
    }
    
    private fun updateConversationLastMessage(conversationId: String, messageId: String, timestamp: Long) {
        val currentConversations = conversationsFlow.value.toMutableList()
        val index = currentConversations.indexOfFirst { it.id == conversationId }
        
        if (index != -1) {
            val conversation = currentConversations[index]
            
            // Create updated conversation with new last message info
            val updatedConversation = conversation.copy(
                lastMessageId = messageId,
                lastMessageTime = timestamp,
                // Increment unread count for recipient
                unreadTwo = conversation.unreadTwo + 1 
            )
            
            // Replace in the list
            currentConversations[index] = updatedConversation
            conversationsFlow.value = currentConversations
        }
    }

    override suspend fun decryptMessage(message: Message, pin: String): String {
        if (message.pinHash != null) {
            // Verify the pin hash matches
            if (message.pinHash != generatePinHash(pin)) {
                throw SecurityException("Invalid PIN")
            }
            
            try {
                // Use AESAlgorithm to decrypt the message
                return AESAlgorithm.AES.decrypt(message.encodedText, pin)
            } catch (e: Exception) {
                throw SecurityException("Decryption failed: ${e.message}")
            }
        } else {
            // Message isn't encrypted
            return message.encodedText
        }
    }
    
    override suspend fun getUserById(userId: String): User? {
        return usersFlow.value.find { it.id == userId }
    }
    
    override suspend fun markConversationAsRead(conversationId: String) {
        val currentConversations = conversationsFlow.value.toMutableList()
        val index = currentConversations.indexOfFirst { it.id == conversationId }
        
        if (index != -1) {
            val conversation = currentConversations[index]
            
            // Update only if current user is participant one
            if (conversation.participantOneId == DataUtils.currentUser.id) {
                val updatedConversation = conversation.copy(unreadOne = 0)
                currentConversations[index] = updatedConversation
                conversationsFlow.value = currentConversations
            } else {
                val updatedConversation = conversation.copy(unreadTwo = 0)
                currentConversations[index] = updatedConversation
                conversationsFlow.value = currentConversations
            }
        }
    }
    
    private fun generatePinHash(pin: String): String {
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray())
            return hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback to simple hash if crypto fails
            return (pin.hashCode() xor 0x5f3759df).toString()
        }
    }
}
