package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageListener: ListenerRegistration
    private lateinit var chatId: String
    private lateinit var receiverId: String
    private lateinit var receiverName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // UI Elements
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val tvReceiverName: TextView = findViewById(R.id.tvReceiverName)
        messagesRecyclerView = findViewById(R.id.rvMessages)
        val messageInput: EditText = findViewById(R.id.etMessage)
        val sendButton: Button = findViewById(R.id.btnSend)

        // Set up the Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // Get chatId from intent
        chatId = intent.getStringExtra("CHAT_ID") ?: run {
            Toast.makeText(this, "Chat ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get current user ID
        val currentUserId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Adapter with currentUserId
        messagesAdapter = MessagesAdapter(emptyList(), currentUserId)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Stack messages from the bottom (newest at the bottom)
        }
        messagesRecyclerView.adapter = messagesAdapter

        // Fetch chat details to get the receiver's ID and name
        db.collection("chats")
            .document(chatId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val participants = document.get("participants") as? List<String> ?: emptyList()
                    receiverId = participants.firstOrNull { it != currentUserId } ?: run {
                        Toast.makeText(this, "Receiver not found", Toast.LENGTH_SHORT).show()
                        finish()
                        return@addOnSuccessListener
                    }

                    val participantInfo = document.get("participantInfo") as? Map<String, Map<String, String>>
                    val receiverName = (participantInfo?.get(receiverId)?.get("name") as? String ?: "Unknown")
                        .replaceFirstChar { it.uppercase() }

                    // Set the receiver's name in the Toolbar
                    tvReceiverName.text = receiverName

                    // Make the Toolbar clickable to view the receiver's profile
                    toolbar.setOnClickListener {
                        val intent = Intent(this, ProfileViewActivity::class.java)
                        intent.putExtra("USER_ID", receiverId)
                        startActivity(intent)
                    }

                    // Load messages
                    loadMessages(chatId)
                } else {
                    Toast.makeText(this, "Chat not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Error fetching chat: ${e.message}", e)
                Toast.makeText(this, "Error loading chat: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }

        // Send button click
        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(chatId, text)
                messageInput.text.clear()
            }
        }
    }

    private fun loadMessages(chatId: String) {
        messageListener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatActivity", "Error listening for messages: ${e.message}", e)
                    Toast.makeText(this, "Error loading messages: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(messageId = doc.id)
                } ?: emptyList()

                messagesAdapter.updateMessages(messages)
                messagesRecyclerView.scrollToPosition(messages.size - 1)

                // Update the last message in the chat document
                if (messages.isNotEmpty()) {
                    val lastMessage = messages.last()
                    val updates = hashMapOf<String, Any?>(
                        "lastMessageText" to lastMessage.text,
                        "lastMessageTimestamp" to lastMessage.timestamp
                    )
                    db.collection("chats").document(chatId).update(updates)
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to update chat: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun sendMessage(chatId: String, text: String) {
        val senderId = auth.currentUser?.uid ?: return
        val message = hashMapOf(
            "senderId" to senderId,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats").document(chatId).collection("messages").add(message)
            .addOnSuccessListener {
                // Update the last message in the chat document (already handled in the snapshot listener)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener.remove() // Stop listening for messages to prevent memory leaks
    }
}