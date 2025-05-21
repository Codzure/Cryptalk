package com.codzure.cryptalk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import com.codzure.cryptalk.data.Message
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MessageAdapter(
    private var messages: List<Message> = emptyList(),
    private val currentUserId: String = "",
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

    // Date formatter for timestamps
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        // Compare senderId with currentUserId to determine if message is sent or received
        return if (messages[position].senderId == currentUserId) {
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

        // Determine if we should show timestamp (first message or >10 min from previous)
        val showTimestamp = position == 0 ||
                (messages[position].timestamp - messages[position - 1].timestamp) > 600000 || // 10 minutes
                !isSameDay(messages[position].timestamp, messages[position - 1].timestamp)

        // Determine if we should group messages from the same sender
        val showSenderInfo = position == 0 ||
                messages[position].senderId != messages[position - 1].senderId

        when (holder) {
            is SenderViewHolder -> holder.bind(message, position, showTimestamp)
            is ReceiverViewHolder -> holder.bind(message, position, showSenderInfo, showTimestamp)
        }
    }

    // Check if two timestamps are on the same day
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val date1 = dateFormat.format(Date(timestamp1))
        val date2 = dateFormat.format(Date(timestamp2))
        return date1 == date2
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
            userColors[userId] = colorOptions[abs(randomIndex)].toColorInt()
        }
        return userColors[userId]!!
    }

    inner class SenderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val messageTime: TextView = view.findViewById(R.id.messageTime)
        private val deliveryStatus: ImageView = view.findViewById(R.id.deliveryStatus)
        private val bubbleContainer: LinearLayout = view.findViewById(R.id.bubbleContainer)
        private val encryptionIcon: ImageView? = view.findViewById(R.id.encryptionIcon)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMessageClicked(messages[pos], bubbleContainer)
                }
            }
        }

        fun bind(message: Message, position: Int, showTimestamp: Boolean) {
            // Handle encrypted messages
            if (message.pinHash != null) {
                messageText.text = "🔒 Encrypted message (tap to decrypt)"
                encryptionIcon?.visibility = View.VISIBLE
            } else {
                messageText.text = message.encodedText
                encryptionIcon?.visibility = View.GONE
            }

            // Set timestamp
            messageTime.text = timeFormat.format(Date(message.timestamp))
            messageTime.visibility = if (showTimestamp) View.VISIBLE else View.GONE

            // Show delivery status
            deliveryStatus.visibility = View.VISIBLE

            // Determine message status (read, delivered, sent)
            when {
                message.isRead -> {
                    // Message has been read
                    deliveryStatus.setImageResource(R.drawable.ic_check_double)
                    deliveryStatus.alpha = 1.0f
                }

                message.isDelivered -> {
                    // Message delivered but not read
                    deliveryStatus.setImageResource(R.drawable.ic_check_double)
                    deliveryStatus.alpha = 0.7f
                }

                else -> {
                    // Message sent but not delivered
                    deliveryStatus.setImageResource(R.drawable.ic_check_single)
                    deliveryStatus.alpha = 0.5f
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
        private val encryptionIcon: ImageView? = view.findViewById(R.id.encryptionIcon)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMessageClicked(messages[pos], bubbleContainer)
                }
            }
        }

        fun bind(message: Message, position: Int, showSenderInfo: Boolean, showTimestamp: Boolean) {
            // Handle encrypted messages
            if (message.pinHash != null) {
                messageText.text = "🔒 Encrypted message (tap to decrypt)"
                encryptionIcon?.visibility = View.VISIBLE
            } else {
                messageText.text = message.encodedText
                encryptionIcon?.visibility = View.GONE
            }

            // Set timestamp
            messageTime.text = timeFormat.format(Date(message.timestamp))
            messageTime.visibility = if (showTimestamp) View.VISIBLE else View.GONE

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

            // Mark message as read if we're displaying it
            if (!message.isRead && message.recipientId == currentUserId) {
                // In a real app, we would update the message status here
                // This UI update will happen when the ViewModel refreshes
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
        bubble.alpha = 0f
        bubble.translationY = 50f
        bubble.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .setStartDelay(position * 50L)
            .start()
    }

    inner class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            // Compare all relevant fields
            return oldItem.encodedText == newItem.encodedText &&
                    oldItem.timestamp == newItem.timestamp &&
                    oldItem.isRead == newItem.isRead &&
                    oldItem.pinHash == newItem.pinHash
        }
    }
}