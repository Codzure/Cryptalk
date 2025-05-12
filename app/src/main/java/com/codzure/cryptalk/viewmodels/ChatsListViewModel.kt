package com.codzure.cryptalk.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codzure.cryptalk.api.ChatRepository
import com.codzure.cryptalk.data.ConversationResponse
import com.codzure.cryptalk.data.User
import com.codzure.cryptalk.models.ConversationUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the ChatsListFragment
 * Handles loading and managing conversations data
 */
class ChatsListViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Conversations data
    private val _conversations = MutableStateFlow<List<ConversationUI>>(emptyList())
    val conversations: StateFlow<List<ConversationUI>> = _conversations.asStateFlow()

    // Raw conversation responses
    private val _rawConversations = MutableStateFlow<List<ConversationResponse>>(emptyList())

    init {
        loadConversations()
    }

    /**
     * Load conversations from repository
     */
    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = repository.getConversations()

                if (result.isSuccess) {
                    val conversationsList = result.getOrNull() ?: emptyList()
                    _rawConversations.value = conversationsList

                    // Transform API conversations to UI models
                    _conversations.value = conversationsList.map { convResponse ->
                        ConversationUI(
                            id = convResponse.conversation.id,
                            userId = convResponse.participant.id,
                            userName = convResponse.participant.fullName,
                            lastMessage = convResponse.lastMessage?.encodedText ?: "No messages",
                            lastMessageTime = convResponse.conversation.lastMessageTime,
                            isEncrypted = convResponse.lastMessage?.pinHash != null,
                            unreadCount = calculateUnreadCount(convResponse)
                        )
                    }
                } else {
                    _error.value =
                        "Failed to load conversations: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load conversations: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new conversation with a user
     */
    fun createConversation(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = repository.createConversation(userId)

                if (result.isSuccess) {
                    // Refresh conversations list
                    loadConversations()
                } else {
                    _error.value =
                        "Failed to create conversation: ${result.exceptionOrNull()?.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to create conversation: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Search for users to start a conversation with
     */
    fun searchUsers(query: String, onResult: (List<User>) -> Unit) {
        if (query.length < 3) return // Don't search for very short queries

        viewModelScope.launch {
            try {
                val result = repository.searchUsers(query)

                if (result.isSuccess) {
                    val users = result.getOrNull() ?: emptyList()
                    onResult(users)
                } else {
                    _error.value = "Search failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            }
        }
    }

    /**
     * Calculate the number of unread messages for a conversation
     */
    private fun calculateUnreadCount(conversationResponse: ConversationResponse): Int {
        val currentUser = repository.getCurrentUser()
        return if (currentUser != null) {
            val conv = conversationResponse.conversation
            if (conv.participantOneId == currentUser.id) {
                conv.unreadOne
            } else {
                conv.unreadTwo
            }
        } else {
            0
        }
    }

    /**
     * Get a conversation response by ID
     */
    fun getConversationById(id: String): ConversationResponse? {
        return _rawConversations.value.find { it.conversation.id == id }
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }
}
