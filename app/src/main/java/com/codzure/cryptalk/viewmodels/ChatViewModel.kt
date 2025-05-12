package com.codzure.cryptalk.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codzure.cryptalk.api.ChatRepository
import com.codzure.cryptalk.data.Message
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
    private val repository: ChatRepository
) : ViewModel() {
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Message sending state
    private val _messageSendingState = MutableStateFlow(MessageSendState.IDLE)
    val messageSendingState: StateFlow<MessageSendState> = _messageSendingState.asStateFlow()
    
    // Messages data
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    // Recipient user data
    private val _recipient = MutableStateFlow<User?>(null)
    val recipient: StateFlow<User?> = _recipient.asStateFlow()
    
    // Current conversation ID
    private var conversationId: String? = null
    
    // Current user ID (from repository)
    private var currentUserId: String? = null
    
    init {
        // Get current user ID from repository
        val user = repository.getCurrentUser()
        currentUserId = user?.id ?: "current-user-id"
    }
    
    /**
     * Load conversation and messages for a specific conversation ID and recipient
     */
    fun loadConversation(conversationId: String, recipient: User) {
        this.conversationId = conversationId
        _recipient.value = recipient
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Get messages for this conversation
                val result = repository.getMessages(conversationId)
                
                if (result.isSuccess) {
                    val messagesList = result.getOrNull() ?: emptyList()
                    _messages.value = messagesList
                    
                    // Mark unread messages as read
                    val unreadMessageIds = messagesList
                        .filter { !it.isRead && it.recipientId == getCurrentUserId() }
                        .map { it.id }
                    
                    if (unreadMessageIds.isNotEmpty()) {
                        repository.markMessagesAsRead(conversationId, unreadMessageIds)
                    }
                } else {
                    _error.value = "Failed to load messages: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load conversation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a new conversation with a user ID and load it
     * This is for backward compatibility with the previous implementation
     */
    fun createConversationByUserId(userId: String, recipientName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Try to create a conversation with this user
                val result = repository.createConversation(userId)
                
                if (result.isSuccess) {
                    val conversation = result.getOrNull()
                    if (conversation != null) {
                        // Create a temporary user object for the recipient
                        val tempUser = User(
                            id = userId,
                            fullName = recipientName,
                            phoneNumber = "",
                            email = "",
                            username = "",
                            avatarUrl = null
                        )
                        
                        // Load the conversation
                        loadConversation(conversation.conversation.id, tempUser)
                    } else {
                        _error.value = "Failed to create conversation"
                        _isLoading.value = false
                    }
                } else {
                    _error.value = "Failed to create conversation: ${result.exceptionOrNull()?.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to create conversation: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Send a new message in the current conversation
     */
    fun sendMessage(text: String, pin: String? = null) {
        if (conversationId == null || _recipient.value == null) {
            _error.value = "Conversation not initialized"
            return
        }
        
        viewModelScope.launch {
            _messageSendingState.value = MessageSendState.SENDING
            
            try {
                val result = repository.sendMessage(
                    conversationId = conversationId!!,
                    recipientId = _recipient.value!!.id,
                    text = text,
                    pinHash = pin
                )
                
                if (result.isSuccess) {
                    _messageSendingState.value = MessageSendState.SUCCESS
                    // Refresh messages to include the new one
                    loadMessagesFromRepository()
                } else {
                    _error.value = "Failed to send message: ${result.exceptionOrNull()?.message}"
                    _messageSendingState.value = MessageSendState.ERROR
                }
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
                _messageSendingState.value = MessageSendState.ERROR
            }
        }
    }
    
    /**
     * Decrypt a message with a PIN
     */
    fun decryptMessage(message: Message, pin: String, callback: (String?, Boolean) -> Unit) {
        if (message.pinHash == null) {
            callback("This message is not encrypted", false)
            return
        }
        
        viewModelScope.launch {
            try {
                // For security, we'd typically handle decryption on the server
                // This is a simplified implementation
                
                // Here we're treating the pinHash not as a true hash but as a PIN
                // In a real app, this would be a proper cryptographic operation
                if (message.pinHash == pin) {
                    callback(message.encodedText, true)
                } else {
                    callback(null, false)
                }
            } catch (e: Exception) {
                callback("Error decrypting message: ${e.message}", false)
            }
        }
    }
    
    /**
     * Refreshes messages from the repository
     * @param silent If true, does not show loading indicators
     */
    fun refreshMessages(silent: Boolean = false) {
        if (conversationId == null) {
            _error.value = "Conversation not initialized"
            return
        }
        
        loadMessagesFromRepository(refresh = true, silent = silent)
    }
    
    /**
     * Load messages from the repository
     * @param refresh Force a refresh from the server
     * @param silent If true, does not show loading indicators
     */
    private fun loadMessagesFromRepository(refresh: Boolean = false, silent: Boolean = false) {
        if (conversationId == null) return
        
        viewModelScope.launch {
            if (!silent) {
                _isLoading.value = true
            }
            
            try {
                val result = repository.getMessages(conversationId!!, refresh)
                
                if (result.isSuccess) {
                    _messages.value = result.getOrNull() ?: emptyList()
                    
                    // Mark unread messages as read
                    val unreadMessageIds = _messages.value
                        .filter { !it.isRead && it.recipientId == getCurrentUserId() }
                        .map { it.id }
                    
                    if (unreadMessageIds.isNotEmpty()) {
                        repository.markMessagesAsRead(conversationId!!, unreadMessageIds)
                    }
                } else if (!silent) {
                    _error.value = "Failed to load messages: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                if (!silent) {
                    _error.value = "Failed to load messages: ${e.message}"
                }
            } finally {
                if (!silent) {
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Get the current user ID
     */
    fun getCurrentUserId(): String {
        return currentUserId ?: "current-user-id"
    }
    
    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clear message cache for this conversation when ViewModel is cleared
        if (conversationId != null) {
            repository.clearMessageCache(conversationId)
        }
    }
    
    /**
     * Possible states for message sending
     */
    enum class MessageSendState {
        IDLE,
        SENDING,
        SUCCESS,
        ERROR
    }
}
