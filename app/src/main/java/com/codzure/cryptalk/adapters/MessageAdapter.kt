package com.codzure.cryptalk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import com.codzure.cryptalk.data.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private var messages: List<Message> = emptyList(),
    private val currentUserNumber: String = "1234567890", // Example user number
    private val onMessageClicked: (Message, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENDER = 1
    private val VIEW_TYPE_RECEIVER = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserNumber) {
            VIEW_TYPE_SENDER
        } else {
            VIEW_TYPE_RECEIVER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_bubble_sender, parent, false)
                SenderViewHolder(view)
            }
            VIEW_TYPE_RECEIVER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_bubble_receiver, parent, false)
                ReceiverViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SenderViewHolder) {
            holder.bind(message, position)
        } else if (holder is ReceiverViewHolder) {
            holder.bind(message, position)
        }
    }

    override fun getItemCount(): Int = messages.size
    
    /**
     * Submit a new list of messages and calculate the differences with DiffUtil
     */
    fun submitList(newMessages: List<Message>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        this.messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }

    inner class SenderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val messageTime: TextView = view.findViewById(R.id.messageTime)
        private val senderInfo: TextView = view.findViewById(R.id.senderInfo)
        private val bubbleContainer: LinearLayout = view.findViewById(R.id.bubbleContainer)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMessageClicked(messages[pos], it)
                }
            }
        }

        fun bind(message: Message, pos: Int) {
            messageText.text = if (message.pinHash != null) "🔒 Encrypted message" else message.encodedText
            senderInfo.text = "${message.senderId} • ${message.senderId}"
            messageTime.text = timeFormat.format(Date(message.timestamp))

            bubbleContainer.setBackgroundResource(R.drawable.bg_message_bubble_self)
            
            // Only animate new items
            if (pos >= itemCount - 1) {
                animateBubble(bubbleContainer, pos)
            }
        }
    }

    inner class ReceiverViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val messageTime: TextView = view.findViewById(R.id.messageTime)
        private val senderInfo: TextView = view.findViewById(R.id.senderInfo)
        private val senderInitials: TextView = view.findViewById(R.id.senderInitials)
        private val bubbleContainer: LinearLayout = view.findViewById(R.id.bubbleContainer)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMessageClicked(messages[pos], it)
                }
            }
        }

        fun bind(message: Message, position: Int) {
            messageText.text = if (message.pinHash != null) "🔒 Encrypted message" else message.encodedText
            senderInfo.text = "${message.senderId} • ${message.senderId}"
            messageTime.text = timeFormat.format(Date(message.timestamp))

            senderInitials.text = message.senderId
                .split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .take(2)

            bubbleContainer.setBackgroundResource(R.drawable.bg_message_bubble)
            
            // Only animate new items
            if (position >= itemCount - 1) {
                animateBubble(bubbleContainer, position)
            }
        }
    }

    private fun animateBubble(bubble: LinearLayout, position: Int) {
        bubble.apply {
            translationX = if (position % 2 == 0) 100f else -100f
            alpha = 0f
            animate().translationX(0f).alpha(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
    
    /**
     * DiffUtil callback for calculating differences between lists
     */
    private class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize() = oldList.size
        
        override fun getNewListSize() = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    companion object {
        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    }
}