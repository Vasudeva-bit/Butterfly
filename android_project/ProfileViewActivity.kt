package com.example.android_project

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewActivity : AppCompatActivity() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_view)

        // UI Elements
        val tvProfileType: TextView = findViewById(R.id.tvProfileType)
        val tvEmail: TextView = findViewById(R.id.tvEmail)
        val tvName: TextView = findViewById(R.id.tvName)
        val tvFunding: TextView = findViewById(R.id.tvFunding)
        val tvDetails: TextView = findViewById(R.id.tvDetails)
        val tvPreferences: TextView = findViewById(R.id.tvPreferences)
        val tvUserType: TextView = findViewById(R.id.tvUserType)
        // Get the userId from the intent
        val userId = intent.getStringExtra("USER_ID")

        if (userId == null) {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Fetch the user's profile
        fetchUserProfile(userId, tvProfileType, tvEmail, tvName, tvFunding, tvDetails, tvPreferences, tvUserType)
    }

    private fun fetchUserProfile(
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
                    val companyName = document.getString("oname") ?: "N/A"
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
//
                                            tvProfileType.text = "Mentor Profile"
                                            tvEmail.text = "Email: $email"
                                            tvName.text = "Name: $name"
                                            tvFunding.text = "Fee: $fee"
                                            tvDetails.text = "Hours per Week: $hours"
                                            tvPreferences.text = "Preferences: $industry"
                                            tvUserType.text = "User Type: $userType"
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
                                                    } else {
                                                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                                                        finish()
                                                    }
                                                }
                                                .addOnFailureListener { exception ->
                                                    Log.e("ProfileView", "Error fetching investor profile: ${exception.message}", exception)
                                                    Toast.makeText(this, "Error fetching profile: ${exception.message}", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("ProfileView", "Error fetching mentor profile: ${exception.message}", exception)
                                        Toast.makeText(this, "Error fetching profile: ${exception.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ProfileView", "Error fetching startup profile: ${exception.message}", exception)
                            Toast.makeText(this, "Error fetching profile: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileView", "Error fetching entrepreneur profile: ${exception.message}", exception)
                Toast.makeText(this, "Error fetching profile: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}