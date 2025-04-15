package com.example.android_project

// User data model (generic for all types)
data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val userType: String = "" // "entrepreneur", "investor", "mentor", "startupOrg"
)

// Chat item for recent chats display
data class ChatItem(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUsername: String = "",
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = 0L,
)

// Message data model for chat messages
data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)