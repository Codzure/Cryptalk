package com.codzure.cryptalk.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val messageText: TextView = view.findViewById(R.id.messageText)

        val senderInfo: TextView = view.findViewById(R.id.senderInfo)
        val messageTime: TextView = view.findViewById(R.id.messageTime)

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
        val displayText = if (message.pinHash != null) "🔒 Encrypted message" else message.encodedText
        holder.messageText.text = displayText
        holder.senderInfo.text = "${message.sender} • ${message.senderNumber}"

        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.messageTime.text = dateFormat.format(Date(message.timestamp))

        // Optional: Styling based on sender or encryption
        if (message.sender == "me") {
            holder.messageText.setBackgroundResource(R.drawable.bg_message_bubble_self)
        } else {
            holder.messageText.setBackgroundResource(R.drawable.bg_message_bubble)
        }
    }

    override fun getItemCount(): Int = messages.size
}