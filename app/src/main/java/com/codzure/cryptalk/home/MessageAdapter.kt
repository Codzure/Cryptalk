package com.codzure.cryptalk.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R

class MessageAdapter(
    private val messages: List<Message>,
    private val onMessageClicked: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)

        init {
            view.setOnClickListener {
                val msg = messages[adapterPosition]
                onMessageClicked(msg)
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
    }

    override fun getItemCount(): Int = messages.size
}