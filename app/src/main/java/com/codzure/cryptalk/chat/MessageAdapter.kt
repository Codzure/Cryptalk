package com.codzure.cryptalk.chat

import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserNumber: String = "1234567890", // 👈 Dynamic comparison
    private val onMessageClicked: (Message, View) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageRoot: LinearLayout = view.findViewById(R.id.messageRoot)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val senderInfo: TextView = view.findViewById(R.id.senderInfo)
        val messageTime: TextView = view.findViewById(R.id.messageTime)
        val senderInitials: TextView = view.findViewById(R.id.senderInitials)
        val bubbleContainer: LinearLayout = view.findViewById(R.id.bubbleContainer)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMessageClicked(messages[pos], it)
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
        val isMe = message.senderNumber == currentUserNumber

        // Text content
        holder.messageText.text = if (message.pinHash != null) "🔒 Encrypted message" else message.encodedText
        holder.senderInfo.text = "${message.sender} • ${message.senderNumber}"
        holder.messageTime.text = timeFormat.format(Date(message.timestamp))

        // Initials
        holder.senderInitials.text = message.sender
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .take(2)

        // Flip layout direction based on sender
        val layoutParams = holder.messageRoot.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = if (isMe) 63 else 0
        layoutParams.marginEnd = if (isMe) 0 else 64
        holder.messageRoot.layoutParams = layoutParams
        holder.messageRoot.gravity = if (isMe) Gravity.END else Gravity.START

        // Update view order dynamically
        holder.messageRoot.removeAllViews()
        if (isMe) {
            holder.messageRoot.addView(holder.bubbleContainer)
        } else {
            //holder.messageRoot.addView(holder.senderInitials)
            holder.messageRoot.addView(holder.bubbleContainer)
        }

        // Slide-in animation
        holder.bubbleContainer.apply {
            translationX = if (isMe) 100f else -100f
            alpha = 0f
            animate().translationX(0f).alpha(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()

            setBackgroundResource(
                if (isMe) R.drawable.bg_message_bubble_self else R.drawable.bg_message_bubble
            )
        }
    }


    override fun getItemCount(): Int = messages.size

    companion object {
        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    }
}
