package com.codzure.cryptalk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import com.codzure.cryptalk.data.Message
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class MessageAdapter(
    private var messages: List<Message> = emptyList(),
    private val currentUserNumber: String = "1234567890", // Example user number
    private val onMessageClicked: (Message, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Create a map to store random colors for user avatars
    private val userColors = mutableMapOf<String, Int>()
    private val colorOptions = arrayOf(
        "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
        "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#FFC107"
    )

    private val VIEW_TYPE_SENDER = 1
    private val VIEW_TYPE_RECEIVER = 2

    override fun getItemViewType(position: Int): Int {
        // Compare senderId with currentUserNumber to determine if message is sent or received
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
        
        // Determine if we should show timestamp (first message or >1 min from previous)
        val showTimestamp = position == 0 || 
                (messages[position].timestamp - messages[position - 1].timestamp) > 60000
        
        // Determine if we should group messages from the same sender
        val showSenderInfo = position == 0 || 
                messages[position].senderId != messages[position - 1].senderId
        
        when (holder) {
            is SenderViewHolder -> holder.bind(message, position, showTimestamp)
            is ReceiverViewHolder -> holder.bind(message, position, showSenderInfo, showTimestamp)
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

    // Get a consistent color for a user
    private fun getColorForUser(userId: String): Int {
        if (!userColors.containsKey(userId)) {
            val randomIndex = userId.hashCode() % colorOptions.size
            userColors[userId] = android.graphics.Color.parseColor(colorOptions[Math.abs(randomIndex)])
        }
        return userColors[userId]!!
    }

    inner class SenderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val messageTime: TextView = view.findViewById(R.id.messageTime)
        private val deliveryStatus: ImageView = view.findViewById(R.id.deliveryStatus)
        private val bubbleContainer: LinearLayout = view.findViewById(R.id.bubbleContainer)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMessageClicked(messages[pos], it)
                }
            }
        }

        fun bind(message: Message, position: Int, showTimestamp: Boolean) {
            messageText.text = if (message.pinHash != null) "🔒 Encrypted message" else message.encodedText
            messageTime.text = timeFormat.format(Date(message.timestamp))
            
            // Show delivery status
            deliveryStatus.visibility = View.VISIBLE
            
            // Determine message status (sending, sent, delivered, read)
            // This is mocked for the demo - in a real app, get this from message
            when {
                message.timestamp > System.currentTimeMillis() - 10000 -> {
                    // Recently sent message - single check
                    deliveryStatus.setImageResource(R.drawable.ic_check_double)
                    deliveryStatus.alpha = 0.5f
                }
                else -> {
                    // Older message - double check
                    deliveryStatus.setImageResource(R.drawable.ic_check_double)
                    deliveryStatus.alpha = 1.0f
                }
            }
            
            // Only animate new items
            if (position >= itemCount - 1) {
                animateBubble(bubbleContainer, position)
            }
        }
    }

    inner class ReceiverViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val messageTime: TextView = view.findViewById(R.id.messageTime)
        private val senderInfo: TextView = view.findViewById(R.id.senderInfo)
        private val senderInitials: TextView = view.findViewById(R.id.senderInitials)
        private val senderAvatar: CircleImageView? = view.findViewById(R.id.senderAvatar)
        private val bubbleContainer: LinearLayout = view.findViewById(R.id.bubbleContainer)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMessageClicked(messages[pos], it)
                }
            }
        }

        fun bind(message: Message, position: Int, showSenderInfo: Boolean, showTimestamp: Boolean) {
            messageText.text = if (message.pinHash != null) "🔒 Encrypted message" else message.encodedText
            messageTime.text = timeFormat.format(Date(message.timestamp))

            // Only show sender info if it's the first message in a group from the same sender
            if (showSenderInfo) {
                senderInfo.visibility = View.VISIBLE
                senderInfo.text = formatSenderName(message.senderId)
                
                // Get color for this sender and apply to initial avatar
                val senderColor = getColorForUser(message.senderId)
                senderInitials.setBackgroundColor(senderColor)
                
                // Show sender initials for received messages
                senderInitials.visibility = View.VISIBLE
                senderInitials.text = message.senderId
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .take(2)
            } else {
                senderInfo.visibility = View.GONE
                senderInitials.visibility = View.GONE
            }
            
            // Only animate new items
            if (position >= itemCount - 1) {
                animateBubble(bubbleContainer, position)
            }
        }
        
        // Format sender phone number or name for display
        private fun formatSenderName(senderId: String): String {
            // In a real app, get from contacts
            return if (senderId.matches(Regex("\\d+"))) {
                "+${senderId.substring(0, 3)} ${senderId.substring(3)}"
            } else {
                senderId
            }
        }
    }

    private fun animateBubble(bubble: LinearLayout, position: Int) {
        bubble.apply {
            translationX = if (position % 2 == 0) 30f else -30f
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