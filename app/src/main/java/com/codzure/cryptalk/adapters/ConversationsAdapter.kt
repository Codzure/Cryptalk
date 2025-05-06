package com.codzure.cryptalk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import com.codzure.cryptalk.data.Conversation
import com.codzure.cryptalk.databinding.ItemConversationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationsAdapter(
    private val onClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationsAdapter.ConversationViewHolder>(CONVERSATION_COMPARATOR) {

    inner class ConversationViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(conversation: Conversation) {
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
        private val CONVERSATION_COMPARATOR = object : DiffUtil.ItemCallback<Conversation>() {
            override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
                return oldItem.userId == newItem.userId
            }

            override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
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
    fun submitConversations(conversations: List<Conversation>) {
        submitList(conversations)
    }
}