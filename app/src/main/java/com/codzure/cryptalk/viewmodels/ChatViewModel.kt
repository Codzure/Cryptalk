package com.codzure.cryptalk.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codzure.cryptalk.data.Message
import com.codzure.cryptalk.data.MessageRepository
import com.codzure.cryptalk.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the ChatFragment
 * Handles loading and managing messages for a specific conversation
 */
class ChatViewModel(
    private val repository: MessageRepository
) : ViewModel() {
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Messages data
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    // Recipient user data
    private val _recipient = MutableStateFlow<User?>(null)
    val recipient: StateFlow<User?> = _recipient.asStateFlow()
    
    // Current conversation ID
    private var conversationId: String? = null
    
    /**
     * Load conversation and messages for a specific recipient
     */
    fun loadConversation(recipientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Load recipient user data
                val user = repository.getUserById(recipientId)
                _recipient.value = user
                
                // Find conversation ID - in a real app this would be more robust
                repository.getConversations().collect { conversations ->
                    // Look for conversation with this recipient
                    val conversation = conversations.find { conv ->
                        conv.participantOneId == recipientId || conv.participantTwoId == recipientId
                    }
                    
                    if (conversation != null) {
                        conversationId = conversation.id
                        // Mark conversation as read
                        repository.markConversationAsRead(conversation.id)
                        
                        // Load messages for this conversation
                        repository.getMessages(conversation.id).collect { messagesList ->
                            _messages.value = messagesList
                            _isLoading.value = false
                        }
                    } else {
                        _error.value = "Conversation not found"
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load conversation: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Send a new message in the current conversation
     */
    fun sendMessage(text: String, encrypt: Boolean = false, pin: String? = null) {
        if (conversationId == null || _recipient.value == null) {
            _error.value = "Conversation not initialized"
            return
        }
        
        viewModelScope.launch {
            try {
                repository.sendMessage(
                    conversationId = conversationId!!,
                    recipientId = _recipient.value!!.id,
                    text = text,
                    encrypt = encrypt,
                    pin = pin
                )
                
                // Message will be updated via flow collection
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }
    
    /**
     * Decrypt a message using a provided PIN
     */
    fun decryptMessage(message: Message, pin: String, onDecrypted: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val decryptedText = repository.decryptMessage(message, pin)
                onDecrypted(decryptedText)
            } catch (e: Exception) {
                _error.value = "Decryption failed: ${e.message}"
            }
        }
    }
    
    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }
}
