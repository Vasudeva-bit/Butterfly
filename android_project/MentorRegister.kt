package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MentorRegister : AppCompatActivity() {

    // Firebase instances
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mentor_register)

        // UI Elements with explicit types
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val etMentorName: EditText = findViewById(R.id.etMentorName)
        val etHours: EditText = findViewById(R.id.etHours)
        val etFees: EditText = findViewById(R.id.etFees)
        val spIndustry: Spinner = findViewById(R.id.spIndustry)
        val btnRegister: Button = findViewById(R.id.btnRegister)

        // Register Button Click Listener
        btnRegister.setOnClickListener {
            val email: String = etEmail.text.toString().trim()
            val password: String = etPassword.text.toString().trim()
            val mentorName: String = etMentorName.text.toString().trim()
            val hours: String = etHours.text.toString().trim()
            val fees: String = etFees.text.toString().trim()
            val industry: String = spIndustry.selectedItem.toString()

            // Validation
            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (mentorName.isEmpty()) {
                etMentorName.error = "Mentor name is required"
                return@setOnClickListener
            }
            if (hours.isEmpty()) {
                etHours.error = "Hours available is required"
                return@setOnClickListener
            }
            if (fees.isEmpty()) {
                etFees.error = "Expected fees is required"
                return@setOnClickListener
            }
            if (industry == "Select Industry") {
                Toast.makeText(this, "Please select an industry", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show progress and disable button
            btnRegister.isEnabled = false

            // Step 1: Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user?.uid ?: return@addOnSuccessListener

                    // Step 2: Save mentor profile to Firestore
                    val mentor: HashMap<String, Any> = hashMapOf(
                        "email" to email,
                        "oname" to mentorName,
                        "name" to mentorName.lowercase(),
                        "hoursAvailable" to (hours.toIntOrNull() ?: 0),
                        "feesPerHour" to (fees.toDoubleOrNull() ?: 0.0),
                        "industry" to industry,
                        "timestamp" to System.currentTimeMillis(),
                        "userId" to userId,
                        "userType" to "mentors"
                    )

                    db.collection("mentors")
                        .document(userId) // Use userId as document ID for easier lookup
                        .set(mentor)
                        .addOnSuccessListener {
                            btnRegister.isEnabled = true
                            Toast.makeText(
                                this,
                                "Registration successful!",
                                Toast.LENGTH_LONG
                            ).show()
                            clearForm(etEmail, etPassword, etMentorName, etHours, etFees, spIndustry)
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            btnRegister.isEnabled = true
                            Toast.makeText(
                                this,
                                "Error saving profile: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { exception ->
                    btnRegister.isEnabled = true
                    Toast.makeText(
                        this,
                        "Error creating account: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    // Helper function with explicit parameter types
    private fun clearForm(
        etEmail: EditText,
        etPassword: EditText,
        etMentorName: EditText,
        etHours: EditText,
        etFees: EditText,
        spIndustry: Spinner
    ) {
        etEmail.text.clear()
        etPassword.text.clear()
        etMentorName.text.clear()
        etHours.text.clear()
        etFees.text.clear()
        spIndustry.setSelection(0)
    }
}