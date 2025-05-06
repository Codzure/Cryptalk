package com.codzure.cryptalk.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codzure.cryptalk.data.Conversation
import com.codzure.cryptalk.data.MessageRepository
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
    private val repository: MessageRepository
) : ViewModel() {
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Conversations data
    private val _conversations = MutableStateFlow<List<ConversationUI>>(emptyList())
    val conversations: StateFlow<List<ConversationUI>> = _conversations.asStateFlow()
    
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
                repository.getConversationsUI().collect { conversationsList ->
                    _conversations.value = conversationsList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load conversations: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Mark a conversation as read
     */
    fun markConversationAsRead(conversationId: String) {
        viewModelScope.launch {
            try {
                repository.markConversationAsRead(conversationId)
            } catch (e: Exception) {
                _error.value = "Failed to mark conversation as read: ${e.message}"
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
