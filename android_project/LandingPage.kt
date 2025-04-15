package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class LandingPage : AppCompatActivity() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var searchAdapter: UsersAdapter
    private var isNavigating = false // Flag to prevent multiple navigations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)

        // UI Elements
        val profileButton: ImageButton = findViewById(R.id.btnProfile)
        val searchBar: EditText = findViewById(R.id.etSearch)
        val greetingText: TextView = findViewById(R.id.tvGreeting)
        val chatsRecyclerView: RecyclerView = findViewById(R.id.rvChats)
        val searchResultsRecyclerView: RecyclerView = findViewById(R.id.rvSearchResults)

        // Initialize Adapters
        chatsAdapter = ChatsAdapter(emptyList()) { chatId ->
            navigateToChat(chatId)
        }
        searchAdapter = UsersAdapter(emptyList()) { user ->
            startChatWithUser(user)
        }

        // Set up RecyclerViews
        chatsRecyclerView.layoutManager = LinearLayoutManager(this)
        chatsRecyclerView.adapter = chatsAdapter
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = searchAdapter

        // Get current user
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        // Set greeting
        greetingText.text = "Welcome, ${currentUser.email?.split("@")?.get(0) ?: "User"}!"

        // Load chats
        loadRecentChats(currentUser.uid)

        // Profile button click
        profileButton.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        // Search bar listener
        searchBar.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                searchUsers(query, searchResultsRecyclerView, chatsRecyclerView)
            } else {
                searchAdapter.updateUsers(emptyList())
                searchResultsRecyclerView.visibility = View.GONE
                chatsRecyclerView.visibility = View.VISIBLE
            }
            true
        }
    }

    private fun loadRecentChats(userId: String) {
        db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("LoadRecentChats", "Error loading chats: ${e.message}", e)
                    Toast.makeText(this, "Error loading chats: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val participants = doc.get("participants") as? List<String> ?: return@mapNotNull null
                        val otherUserId = participants.firstOrNull { it != userId } ?: return@mapNotNull null
                        val lastMessageText = doc.getString("lastMessageText") ?: ""
                        val lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L
                        val participantInfo = doc.get("participantInfo") as? Map<String, Map<String, Any>> ?: return@mapNotNull null
                        val otherUserInfo = participantInfo[otherUserId] ?: return@mapNotNull null
                        val otherUserName = (otherUserInfo["name"] as? String ?: "Unknown").replaceFirstChar { it.uppercase() }
                        ChatItem(doc.id, otherUserId, otherUserName, lastMessageText, lastMessageTimestamp)
                    } catch (ex: Exception) {
                        Log.e("LoadRecentChats", "Error parsing chat document ${doc.id}: ${ex.message}", ex)
                        null
                    }
                }?.sortedByDescending { it.lastMessageTimestamp } ?: emptyList()

                Log.d("LoadRecentChats", "Loaded ${chats.size} chats for user: $userId")
                chatsAdapter.updateChats(chats)
            }
    }

    private fun searchUsers(query: String, searchRv: RecyclerView, chatsRv: RecyclerView) {
        val collections = listOf("entrepreneurs", "startups", "mentors", "investors")
        val results = mutableListOf<User>()
        var completedQueries = 0

        collections.forEach { collection ->
            db.collection(collection)
                .whereGreaterThanOrEqualTo("name", query.lowercase())
                .whereLessThanOrEqualTo("name", query.lowercase() + "\uf8ff")
                .get()
                .addOnSuccessListener { snapshot ->
                    val users = snapshot.documents.mapNotNull { doc ->
                        val userId = doc.id
                        if (userId == auth.currentUser?.uid) null
                        else doc.toObject(User::class.java)?.copy(userId = userId)
                    }
                    results.addAll(users)
                    completedQueries++
                    if (completedQueries == collections.size) {
                        searchAdapter.updateUsers(results)
                        searchRv.visibility = View.VISIBLE
                        chatsRv.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Search failed in $collection: ${e.message}", Toast.LENGTH_SHORT).show()
                    completedQueries++
                }
        }
    }

    private fun startChatWithUser(user: User) {
        if (isNavigating) return // Prevent multiple clicks
        isNavigating = true

        val currentUserId = auth.currentUser?.uid ?: return
        val participants = listOf(currentUserId, user.userId).sorted()
        Log.d("StartChat", "Starting chat with userId: ${user.userId}, participants: $participants")

        // Check if chat already exists
        db.collection("chats")
            .whereEqualTo("participants", participants)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    // Chat exists, navigate to it
                    val chatId = snapshot.documents.first().id
                    Log.d("StartChat", "Existing chat found with ID: $chatId for ${user.name}")
                    navigateToChat(chatId)
                } else {
                    // Create a new chat with participant info
                    getUserInfo(currentUserId) { currentUserType, currentUserName ->
                        getUserInfo(user.userId) { otherUserType, otherUserName ->
                            val chatData = hashMapOf(
                                "participants" to participants,
                                "lastMessageText" to "",
                                "lastMessageTimestamp" to 0L,
                                "participantInfo" to mapOf(
                                    currentUserId to mapOf(
                                        "name" to currentUserName,
                                        "userType" to currentUserType
                                    ),
                                    user.userId to mapOf(
                                        "name" to otherUserName,
                                        "userType" to otherUserType
                                    )
                                )
                            )
                            db.collection("chats")
                                .add(chatData)
                                .addOnSuccessListener { docRef ->
                                    Log.d("StartChat", "New chat created with ID: ${docRef.id}")
                                    navigateToChat(docRef.id)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("StartChat", "Error creating chat: ${e.message}", e)
                                    Toast.makeText(this, "Error starting chat: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isNavigating = false
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("StartChat", "Error checking for existing chat: ${e.message}", e)
                Toast.makeText(this, "Error starting chat: ${e.message}", Toast.LENGTH_SHORT).show()
                isNavigating = false
            }
    }

    private fun navigateToChat(chatId: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("CHAT_ID", chatId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) // Optimize back stack
        }
        startActivity(intent)
        isNavigating = false // Reset flag after navigation
    }

    private fun getUserInfo(userId: String, callback: (String, String) -> Unit) {
        val collections = listOf("entrepreneurs", "startups", "mentors", "investors")
        var userFound = false

        for (collection in collections) {
            db.collection(collection)
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists() && !userFound) {
                        userFound = true
                        val name = document.getString("name") ?: "Unknown"
                        val userType = collection.removeSuffix("s")
                        callback(userType, name)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("GetUserInfo", "Error checking $collection for user $userId: ${e.message}", e)
                }
        }
        // Fallback if no match is found after a delay (simplified for robustness)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!userFound) callback("unknown", "Unknown")
        }, 1000)
    }
}