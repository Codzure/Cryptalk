package com.codzure.cryptalk.chat

import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import com.codzure.cryptalk.data.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserNumber: String = "1234567890", // Example user number
    private val onMessageClicked: (Message, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENDER = 1
    private val VIEW_TYPE_RECEIVER = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderNumber == currentUserNumber) {
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
            senderInfo.text = "${message.sender} • ${message.senderNumber}"
            messageTime.text = timeFormat.format(Date(message.timestamp))

            bubbleContainer.setBackgroundResource(R.drawable.bg_message_bubble_self)
            animateBubble(bubbleContainer, pos)
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
            senderInfo.text = "${message.sender} • ${message.senderNumber}"
            messageTime.text = timeFormat.format(Date(message.timestamp))

            senderInitials.text = message.sender
                .split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .take(2)

            bubbleContainer.setBackgroundResource(R.drawable.bg_message_bubble)
            animateBubble(bubbleContainer, position)
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

    companion object {
        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    }
}
