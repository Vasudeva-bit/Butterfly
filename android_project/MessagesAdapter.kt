package com.example.android_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessagesAdapter(
    private var messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        // Swap the layouts: sent messages on the left, received on the right
        val layoutRes = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent // Left-aligned for current user
        } else {
            R.layout.item_message_received // Right-aligned for others
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tvMessageText)
        private val timestampText: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(message: Message) {
            messageText.text = message.text
            // Format the timestamp (e.g., "12:34 PM")
            val formattedTime = java.text.SimpleDateFormat("hh:mm a").format(java.util.Date(message.timestamp))
            timestampText.text = formattedTime
        }
    }
}