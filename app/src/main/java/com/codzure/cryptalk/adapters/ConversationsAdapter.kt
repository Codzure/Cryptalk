package com.codzure.cryptalk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import com.codzure.cryptalk.models.ConversationUI
import com.codzure.cryptalk.databinding.ItemConversationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationsAdapter(
    private val onClick: (ConversationUI) -> Unit
) : ListAdapter<ConversationUI, ConversationsAdapter.ConversationViewHolder>(CONVERSATION_COMPARATOR) {

    inner class ConversationViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(conversation: ConversationUI) {
            with(binding) {
                // Set main text elements
                nameText.text = conversation.userName
                messageText.text = conversation.lastMessage
                timestampText.text = formatTimestamp(conversation.lastMessageTime)
                
                // Set avatar initial from the first letter of the username
                avatarText.text = conversation.userName.firstOrNull()?.toString() ?: "?"
                
                // Display lock icon for encrypted messages if needed
                lockIcon.visibility = if (conversation.isEncrypted) {
                    ViewGroup.VISIBLE
                } else {
                    ViewGroup.GONE
                }
                
                // Show unread message count if there are any
                if (conversation.unreadCount > 0) {
                    unreadBadge.visibility = ViewGroup.VISIBLE
                    unreadBadge.text = conversation.unreadCount.toString()
                } else {
                    unreadBadge.visibility = ViewGroup.GONE
                }
                
                // Set accessibility descriptions
                root.contentDescription = root.context.getString(
                    R.string.conversation_with_accessibility,
                    conversation.userName
                )
                
                // Set click listener
                root.setOnClickListener {
                    onClick(conversation)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        return ConversationViewHolder(
            ItemConversationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        /**
         * DiffUtil callback to efficiently update the RecyclerView
         */
        private val CONVERSATION_COMPARATOR = object : DiffUtil.ItemCallback<ConversationUI>() {
            override fun areItemsTheSame(oldItem: ConversationUI, newItem: ConversationUI): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ConversationUI, newItem: ConversationUI): Boolean {
                return oldItem == newItem
            }
        }
        
        /**
         * Cached date formatter for efficiency
         */
        private val dateFormatter by lazy {
            SimpleDateFormat("hh:mm a", Locale.getDefault())
        }
    }
    
    /**
     * Format timestamp to a readable time
     */
    private fun formatTimestamp(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }
    
    /**
     * Submit a new list of conversations with DiffUtil support
     */
    fun submitConversations(conversations: List<ConversationUI>) {
        submitList(conversations)
    }
}