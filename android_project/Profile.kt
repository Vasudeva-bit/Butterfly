package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile : AppCompatActivity() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var connectionsAdapter: ChatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // UI Elements
        val tvProfileType: TextView = findViewById(R.id.tvProfileType)
        val tvEmail: TextView = findViewById(R.id.tvEmail)
        val tvName: TextView = findViewById(R.id.tvName)
        val tvFunding: TextView = findViewById(R.id.tvFunding)
        val tvDetails: TextView = findViewById(R.id.tvDetails)
        val tvPreferences: TextView = findViewById(R.id.tvPreferences)
        val tvUserType: TextView = findViewById(R.id.tvUserType)
        val btnLogout: MaterialButton = findViewById(R.id.btnLogout)
        val btnChat: MaterialButton = findViewById(R.id.btnChat)
        val rvRecommendedConnections: RecyclerView = findViewById(R.id.rvRecommendedConnections)

        // Set up RecyclerView for recommended connections
        connectionsAdapter = ChatsAdapter(emptyList()) { userId ->
            startChatWithUser(userId)
        }
        rvRecommendedConnections.layoutManager = LinearLayoutManager(this)
        rvRecommendedConnections.adapter = connectionsAdapter

        // Get current user
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = currentUser.uid

        // Logout button functionality
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        btnChat.setOnClickListener {
            startActivity(Intent(this, ChatBotActivity::class.java))
        }

        // Fetch current user's profile and recommended connections
        fetchUserProfileAndRecommendations(userId, tvProfileType, tvEmail, tvName, tvFunding, tvDetails, tvPreferences, tvUserType)
    }

    private fun fetchUserProfileAndRecommendations(
        userId: String,
        tvProfileType: TextView,
        tvEmail: TextView,
        tvName: TextView,
        tvFunding: TextView,
        tvDetails: TextView,
        tvPreferences: TextView,
        tvUserType: TextView
    ) {
        // Check entrepreneur profile first
        db.collection("entrepreneurs")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Entrepreneur Profile
                    val email = document.getString("email") ?: "N/A"
                    val companyName = document.getString("name") ?: "N/A"
                    val fundingRequired = document.getDouble("fundingRequired") ?: 0.0
                    val mentorNeeded = document.getBoolean("mentorNeeded") ?: false
                    val collabNeeded = document.getBoolean("collabNeeded") ?: false
                    val investorType = document.getString("investorType") ?: "N/A"
                    val userType = document.getString("userType") ?: "N/A"

                    tvProfileType.text = "Entrepreneur Profile"
                    tvEmail.text = "Email: $email"
                    tvName.text = "Company/Idea Name: $companyName"
                    tvFunding.text = "Funding Required: $$fundingRequired"
                    tvDetails.text = "Investor Type: $investorType"
                    tvPreferences.text = "Mentor Needed: ${if (mentorNeeded) "Yes" else "No"}\nCollaboration Needed: ${if (collabNeeded) "Yes" else "No"}"
                    tvUserType.text = "User Type: $userType"

                    // Fetch recommended connections for entrepreneur
                    fetchRecommendedConnections(userType, null, fundingRequired, mentorNeeded)
                } else {
                    // Check startup profile
                    db.collection("startups")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { startupDoc ->
                            if (startupDoc.exists()) {
                                // Startup Profile
                                val email = startupDoc.getString("email") ?: "N/A"
                                val startupName = startupDoc.getString("oname") ?: "N/A"
                                val fundingGoal = startupDoc.getDouble("fundingGoal") ?: 0.0
                                val description = startupDoc.getString("description") ?: "N/A"
                                val industry = startupDoc.getString("industry") ?: "N/A"
                                val openToCollab = startupDoc.getBoolean("openToCollab") ?: false
                                val userType = startupDoc.getString("userType") ?: "N/A"

                                tvProfileType.text = "Startup Profile"
                                tvEmail.text = "Email: $email"
                                tvName.text = "Startup Name: $startupName"
                                tvFunding.text = "Funding Goal: $$fundingGoal"
                                tvDetails.text = "Description: $description\nIndustry: $industry"
                                tvPreferences.text = "Open to Collaboration: ${if (openToCollab) "Yes" else "No"}"
                                tvUserType.text = "User Type: $userType"

                                // Fetch recommended connections for startup
                                fetchRecommendedConnections(userType, industry, fundingGoal, false)
                            } else {
                                // Check mentor profile
                                db.collection("mentors")
                                    .document(userId)
                                    .get()
                                    .addOnSuccessListener { mentorDoc ->
                                        if (mentorDoc.exists()) {
                                            // Mentor Profile
                                            val email = mentorDoc.getString("email") ?: "N/A"
                                            val name = mentorDoc.getString("oname") ?: "N/A"
                                            val industry = mentorDoc.getString("industry") ?: "N/A"
                                            val userType = mentorDoc.getString("userType") ?: "N/A"
                                            val fee = mentorDoc.getDouble("feesPerHour") ?: "N/A"
                                            val hours = mentorDoc.getDouble("hoursAvailable") ?: "N/A"

                                            tvProfileType.text = "Mentor Profile"
                                            tvEmail.text = "Email: $email"
                                            tvName.text = "Name: $name"
                                            tvFunding.text = "Fee: $fee"
                                            tvDetails.text = "Hours per Week: $hours"
                                            tvPreferences.text = "Preferences: $industry"
                                            tvUserType.text = "User Type: $userType"

                                            // Fetch recommended connections for mentor
                                            fetchRecommendedConnections(userType, industry, 0.0, false)
                                        } else {
                                            // Check investor profile
                                            db.collection("investors")
                                                .document(userId)
                                                .get()
                                                .addOnSuccessListener { investorDoc ->
                                                    if (investorDoc.exists()) {
                                                        // Investor Profile
                                                        val email = investorDoc.getString("email") ?: "N/A"
                                                        val name = investorDoc.getString("oname") ?: "N/A"
                                                        val investmentCapacity = investorDoc.getDouble("investmentCapacity") ?: 0.0
                                                        val mentoring = investorDoc.getBoolean("mentoringAvailable") ?: "N/A"
                                                        val userType = investorDoc.getString("userType") ?: "N/A"
                                                        val preferences = investorDoc.getString("preferredIndustry") ?: "N/A"

                                                        tvProfileType.text = "Investor Profile"
                                                        tvEmail.text = "Email: $email"
                                                        tvName.text = "Name: $name"
                                                        tvFunding.text = "Investment Capacity: $$investmentCapacity"
                                                        tvDetails.text = "Mentoring Availability: $mentoring"
                                                        tvPreferences.text = "Preferences: $preferences"
                                                        tvUserType.text = "User Type: $userType"

                                                        // Fetch recommended connections for investor
                                                        fetchRecommendedConnections(userType, null, investmentCapacity, false)
                                                    } else {
                                                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .addOnFailureListener { exception ->
                                                    Toast.makeText(this, "Error fetching investor profile: ${exception.message}", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(this, "Error fetching mentor profile: ${exception.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error fetching startup profile: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching entrepreneur profile: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchRecommendedConnections(userType: String, industry: String?, funding: Double, mentorNeeded: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val recommendedUsers = mutableListOf<ChatItem>()

        // Query entrepreneurs
        db.collection("entrepreneurs")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val userId = doc.id
                    if (userId == currentUserId) return@forEach // Skip the current user

                    val otherUserType = doc.getString("userType") ?: return@forEach
                    val name = doc.getString("oname") ?: "Unknown"
                    val fundingRequired = doc.getDouble("fundingRequired") ?: 0.0
                    val otherMentorNeeded = doc.getBoolean("mentorNeeded") ?: false

                    // Match based on userType, funding, or mentor needs
                    if (otherUserType == userType ||
                        isFundingSimilar(funding, fundingRequired) ||
                        (mentorNeeded && otherUserType == "mentor") ||
                        (otherMentorNeeded && userType == "mentor")
                    ) {
                        recommendedUsers.add(
                            ChatItem(
                                chatId = userId,
                                otherUserId = userId,
                                otherUsername = name,
                                lastMessageText = "", // Empty to show only the name
                                lastMessageTimestamp = 0L // Set to 0 to hide timestamp
                            )
                        )
                    }
                }

                // Query startups
                db.collection("startups")
                    .get()
                    .addOnSuccessListener { startupSnapshot ->
                        startupSnapshot.documents.forEach { doc ->
                            val userId = doc.id
                            if (userId == currentUserId) return@forEach // Skip the current user

                            val otherUserType = doc.getString("userType") ?: return@forEach
                            val name = doc.getString("oname") ?: "Unknown"
                            val otherIndustry = doc.getString("industry")
                            val fundingGoal = doc.getDouble("fundingGoal") ?: 0.0

                            // Match based on userType, industry, or funding
                            if (otherUserType == userType ||
                                (industry != null && otherIndustry == industry) ||
                                isFundingSimilar(funding, fundingGoal) ||
                                (mentorNeeded && otherUserType == "mentor") ||
                                (userType == "investor" && otherUserType == "startup")
                            ) {
                                recommendedUsers.add(
                                    ChatItem(
                                        chatId = userId,
                                        otherUserId = userId,
                                        otherUsername = name,
                                        lastMessageText = "", // Empty to show only the name
                                        lastMessageTimestamp = 0L // Set to 0 to hide timestamp
                                    )
                                )
                            }
                        }

                        // Query mentors
                        db.collection("mentors")
                            .get()
                            .addOnSuccessListener { mentorSnapshot ->
                                mentorSnapshot.documents.forEach { doc ->
                                    val userId = doc.id
                                    if (userId == currentUserId) return@forEach // Skip the current user

                                    val otherUserType = doc.getString("userType") ?: return@forEach
                                    val name = doc.getString("oname") ?: "Unknown"
                                    val otherIndustry = doc.getString("industry")

                                    // Match based on userType, industry, or mentor needs
                                    if (otherUserType == userType ||
                                        (industry != null && otherIndustry == industry) ||
                                        (mentorNeeded && otherUserType == "mentor") ||
                                        (userType == "entrepreneur" && otherUserType == "mentor")
                                    ) {
                                        recommendedUsers.add(
                                            ChatItem(
                                                chatId = userId,
                                                otherUserId = userId,
                                                otherUsername = name,
                                                lastMessageText = "", // Empty to show only the name
                                                lastMessageTimestamp = 0L // Set to 0 to hide timestamp
                                            )
                                        )
                                    }
                                }

                                // Query investors
                                db.collection("investors")
                                    .get()
                                    .addOnSuccessListener { investorSnapshot ->
                                        investorSnapshot.documents.forEach { doc ->
                                            val userId = doc.id
                                            if (userId == currentUserId) return@forEach // Skip the current user

                                            val otherUserType = doc.getString("userType") ?: return@forEach
                                            val name = doc.getString("oname") ?: "Unknown"
                                            val investmentCapacity = doc.getDouble("investmentCapacity") ?: 0.0

                                            // Match based on userType, funding, or investor interest
                                            if (otherUserType == userType ||
                                                isFundingSimilar(funding, investmentCapacity) ||
                                                (userType == "entrepreneur" && otherUserType == "investor") ||
                                                (userType == "startup" && otherUserType == "investor")
                                            ) {
                                                recommendedUsers.add(
                                                    ChatItem(
                                                        chatId = userId,
                                                        otherUserId = userId,
                                                        otherUsername = name,
                                                        lastMessageText = "", // Empty to show only the name
                                                        lastMessageTimestamp = 0L // Set to 0 to hide timestamp
                                                    )
                                                )
                                            }
                                        }

                                        // Update the RecyclerView with recommended connections
                                        connectionsAdapter.updateChats(recommendedUsers)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Profile", "Error fetching investors: ${e.message}", e)
                                        Toast.makeText(this, "Error fetching recommended investors", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Profile", "Error fetching mentors: ${e.message}", e)
                                Toast.makeText(this, "Error fetching recommended mentors", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Profile", "Error fetching startups: ${e.message}", e)
                        Toast.makeText(this, "Error fetching recommended startups", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Profile", "Error fetching entrepreneurs: ${e.message}", e)
                Toast.makeText(this, "Error fetching recommended entrepreneurs", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isFundingSimilar(funding1: Double, funding2: Double): Boolean {
        // Consider funding similar if within 20% of each other
        val lowerBound = funding1 * 0.8
        val upperBound = funding1 * 1.2
        return funding2 in lowerBound..upperBound
    }

    private fun getUserType(userId: String, callback: (String) -> Unit) {
        // Check entrepreneurs collection
        db.collection("entrepreneurs")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback("entrepreneur")
                } else {
                    // Check startups collection
                    db.collection("startups")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { startupDoc ->
                            if (startupDoc.exists()) {
                                callback("startup")
                            } else {
                                // Check mentors collection
                                db.collection("mentors")
                                    .document(userId)
                                    .get()
                                    .addOnSuccessListener { mentorDoc ->
                                        if (mentorDoc.exists()) {
                                            callback("mentor")
                                        } else {
                                            // Check investors collection
                                            db.collection("investors")
                                                .document(userId)
                                                .get()
                                                .addOnSuccessListener { investorDoc ->
                                                    if (investorDoc.exists()) {
                                                        callback("investor")
                                                    } else {
                                                        callback("unknown") // Default if user not found
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("GetUserType", "Error checking investors: ${e.message}", e)
                                                    callback("unknown")
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("GetUserType", "Error checking mentors: ${e.message}", e)
                                        callback("unknown")
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("GetUserType", "Error checking startups: ${e.message}", e)
                            callback("unknown")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("GetUserType", "Error checking entrepreneurs: ${e.message}", e)
                callback("unknown")
            }
    }

    private fun startChatWithUser(otherUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val participants = listOf(currentUserId, otherUserId).sorted() // Sort to ensure consistent chat ID

        // Get user types and names for both users
        getUserType(currentUserId) { currentUserType ->
            getUserType(otherUserId) { otherUserType ->
                // Fetch current user's name
                db.collection(currentUserType + "s").document(currentUserId).get()
                    .addOnSuccessListener { currentUserDoc ->
                        val currentUserName = currentUserDoc.getString("name") ?: "Unknown"

                        // Fetch other user's name
                        db.collection(otherUserType + "s").document(otherUserId).get()
                            .addOnSuccessListener { otherUserDoc ->
                                val otherUserName = otherUserDoc.getString("name") ?: "Unknown"

                                // Check if a chat already exists between these users
                                db.collection("chats")
                                    .whereEqualTo("participants", participants)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        if (snapshot.documents.isNotEmpty()) {
                                            // Chat exists, navigate to it
                                            val chatId = snapshot.documents.first().id
                                            navigateToChat(chatId)
                                        } else {
                                            // Create a new chat
                                            val chatData = hashMapOf(
                                                "participants" to participants,
                                                "lastMessageText" to "",
                                                "lastMessageTimestamp" to 0L,
                                                "participantInfo" to mapOf(
                                                    currentUserId to mapOf(
                                                        "name" to currentUserName,
                                                        "userType" to currentUserType
                                                    ),
                                                    otherUserId to mapOf(
                                                        "name" to otherUserName,
                                                        "userType" to otherUserType
                                                    )
                                                )
                                            )
                                            db.collection("chats")
                                                .add(chatData)
                                                .addOnSuccessListener { docRef ->
                                                    navigateToChat(docRef.id)
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Profile", "Error creating chat: ${e.message}", e)
                                                    Toast.makeText(this, "Error starting chat", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Profile", "Error checking for existing chat: ${e.message}", e)
                                        Toast.makeText(this, "Error starting chat", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Profile", "Error fetching other user name: ${e.message}", e)
                                Toast.makeText(this, "Error starting chat", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Profile", "Error fetching current user name: ${e.message}", e)
                        Toast.makeText(this, "Error starting chat", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun navigateToChat(chatId: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("CHAT_ID", chatId)
        startActivity(intent)
    }
}