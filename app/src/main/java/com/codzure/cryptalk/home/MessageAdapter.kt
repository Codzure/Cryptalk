package com.codzure.cryptalk.home

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val messages: List<Message>,
    private val onMessageClicked: (Message, View) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val messageRoot: LinearLayout = view.findViewById(R.id.messageRoot)
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val senderInfo: TextView = itemView.findViewById(R.id.senderInfo)
        val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        val senderInitials: TextView = itemView.findViewById(R.id.senderInitials)
        val bubbleContainer: LinearLayout = itemView.findViewById(R.id.bubbleContainer) // 👈 Add this

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMessageClicked(messages[position], it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_bubble, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isMe = message.sender == "Leonard Mutugi"

        // Text to display
        holder.messageText.text = if (message.pinHash != null) "🔒 Encrypted message" else message.encodedText
        holder.senderInfo.text = "${message.sender} • ${message.senderNumber}"

        // Time
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.messageTime.text = dateFormat.format(Date(message.timestamp))

        // Initials (from sender name)
        val initials = message.sender
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .take(2)
        holder.senderInitials.text = initials

        // Alignment for chat bubble and avatar
        val layoutParams = holder.messageRoot.layoutParams as RecyclerView.LayoutParams
        holder.messageRoot.gravity = if (isMe) Gravity.END else Gravity.START

        // Optional: flip order of initials and bubble
        val rootLayout = holder.messageRoot
        rootLayout.removeAllViews()

        if (isMe) {
            rootLayout.addView(holder.bubbleContainer)
            //rootLayout.addView(holder.senderInitials)
        } else {
            rootLayout.addView(holder.senderInitials)
            rootLayout.addView(holder.bubbleContainer)
        }

        // Slide-in Based on Sender
        val translationStart = if (isMe) 100f else -100f
        holder.bubbleContainer.apply {
            translationX = translationStart
            alpha = 0f

            animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Background bubble style
        holder.bubbleContainer.setBackgroundResource(
            if (isMe) R.drawable.bg_message_bubble_self else R.drawable.bg_message_bubble
        )
    }

    override fun getItemCount(): Int = messages.size
}