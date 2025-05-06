package com.codzure.cryptalk.utils

import com.codzure.cryptalk.data.Conversation
import com.codzure.cryptalk.data.Message
import com.codzure.cryptalk.data.User
import com.codzure.cryptalk.models.ConversationUI
import java.util.UUID

/**
 * Utility class for generating and processing data throughout the app
 */
object DataUtils {

    // Current user simulation (in a real app, this would come from authentication)
    val currentUser = User(
        id = "current-user-id",
        username = "leonard.m",
        fullName = "Leonard Mutugi",
        email = "leonard@example.com",
        phoneNumber = "1234567890",
        avatarUrl = null
    )

    // Dummy users for conversations
    internal val dummyUsers = listOf(
        User(
            id = "user-101",
            username = "luna.star",
            fullName = "Luna Star",
            phoneNumber = "101",
            email = "luna@example.com"
        ),
        User(
            id = "user-102",
            username = "kai.zen",
            fullName = "Kai Zen",
            phoneNumber = "102",
            email = "kai@example.com"
        ),
        User(
            id = "user-103",
            username = "zara.swift",
            fullName = "Zara Swift",
            phoneNumber = "103",
            email = "zara@example.com"
        ),
        User(
            id = "user-104",
            username = "leo.night",
            fullName = "Leo Night",
            phoneNumber = "104",
            email = "leo@example.com"
        ),
        User(
            id = "user-105",
            username = "nina.sky",
            fullName = "Nina Sky",
            phoneNumber = "105",
            email = "nina@example.com"
        )
    )

    // Generate dummy conversations linked to users
    fun generateDummyConversations(): List<Conversation> {
        return dummyUsers.map { user ->
            Conversation(
                id = "conv-${user.id}",
                participantOneId = currentUser.id,
                participantTwoId = user.id,
                lastMessageId = null, // Will be set later
                lastMessageTime = System.currentTimeMillis() - (0..1000000).random(),
                unreadOne = (0..5).random(),
                unreadTwo = 0
            )
        }
    }

    // Generate dummy messages for a conversation
    fun generateDummyMessages(): List<Message> {
        val messages = mutableListOf<Message>()
        val conversations = generateDummyConversations()
        
        conversations.forEach { conversation ->
            // Find the other user in this conversation
            val otherUser = dummyUsers.first { it.id == conversation.participantTwoId }
            
            // Generate 1-5 messages for each conversation
            val messageCount = (1..5).random()
            for (i in 0 until messageCount) {
                // Determine if this is an outgoing or incoming message
                val isOutgoing = i % 2 == 0
                
                val messageId = UUID.randomUUID().toString()
                val message = Message(
                    id = messageId,
                    conversationId = conversation.id,
                    senderId = if (isOutgoing) currentUser.id else otherUser.id,
                    recipientId = if (isOutgoing) otherUser.id else currentUser.id,
                    encodedText = generateMessageText(isOutgoing, i),
                    pinHash = if (i % 5 == 0) "hashed_pin_${messageId.takeLast(6)}" else null,
                    timestamp = System.currentTimeMillis() - (60000 * (messageCount - i)),
                    isRead = i < messageCount - 1 // Only most recent might be unread
                )
                messages.add(message)
            }
        }
        
        return messages
    }
    
    // Map data models to UI models for display
    fun mapToConversationUI(
        conversation: Conversation, 
        messages: List<Message>,
        users: List<User>
    ): ConversationUI {
        // Get the other participant (not current user)
        val otherUserId = if (conversation.participantOneId == currentUser.id) 
            conversation.participantTwoId else conversation.participantOneId
            
        // Find user data for the other participant
        val otherUser = users.find { it.id == otherUserId } ?: User(
            id = otherUserId,
            username = "unknown",
            fullName = "Unknown User",
            phoneNumber = "000"
        )
        
        // Find the most recent message for this conversation
        val lastMessage = messages
            .filter { it.conversationId == conversation.id }
            .maxByOrNull { it.timestamp }
            
        // Count unread messages
        val unreadCount = if (conversation.participantOneId == currentUser.id) 
            conversation.unreadOne else conversation.unreadTwo
            
        return ConversationUI(
            id = conversation.id,
            userId = otherUserId,
            userName = otherUser.fullName,
            lastMessage = lastMessage?.let { 
                if (it.pinHash != null) "🔒 Encrypted Message" else it.encodedText 
            } ?: "No messages yet",
            lastMessageTime = lastMessage?.timestamp ?: conversation.lastMessageTime,
            isEncrypted = lastMessage?.pinHash != null,
            unreadCount = unreadCount
        )
    }
    
    // Get a user by ID
    fun getUserById(userId: String): User? {
        return if (userId == currentUser.id) currentUser 
        else dummyUsers.find { it.id == userId }
    }
    
    // Get a user by phone number
    fun getUserByPhone(phoneNumber: String): User? {
        return if (phoneNumber == currentUser.phoneNumber) currentUser 
        else dummyUsers.find { it.phoneNumber == phoneNumber }
    }
    
    // Generate dummy message text
    private fun generateMessageText(isOutgoing: Boolean, index: Int): String {
        val outgoingMessages = listOf(
            "Hey, how's it going?",
            "Just checking in. What's up?",
            "Did you see that new movie?",
            "Let's meet up this weekend",
            "I've got some great news to share!"
        )
        
        val incomingMessages = listOf(
            "Not bad, just busy with work",
            "Hey! Good to hear from you",
            "Yeah, it was amazing! We should go watch it",
            "Sure, I'm free on Saturday",
            "Can't wait to hear it! Tell me more"
        )
        
        val encryptedMessages = listOf(
            "Meeting at the usual place. Code: ALPHA",
            "Password for the account: p@ssw0rd123", 
            "The security code is 9876",
            "My credit card PIN is 1234",
            "The secret location is behind the oak tree"
        )
        
        // Every 5th message is encrypted regardless of direction
        return if (index % 5 == 0) {
            encryptedMessages[index % encryptedMessages.size]
        } else if (isOutgoing) {
            outgoingMessages[index % outgoingMessages.size]
        } else {
            incomingMessages[index % incomingMessages.size]
        }
    }
}
