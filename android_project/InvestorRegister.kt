package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InvestorRegister : AppCompatActivity() {

    // Firebase instances
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_investor_register)

        // UI Elements with explicit types
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val etInvestorName: EditText = findViewById(R.id.etInvestorName)
        val etInvestmentCapacity: EditText = findViewById(R.id.etInvestmentCapacity)
        val rgMentoring: RadioGroup = findViewById(R.id.rgMentoring)
        val spIndustry: Spinner = findViewById(R.id.spIndustry)
        val btnRegister: Button = findViewById(R.id.btnRegister)

        // Register Button Click Listener
        btnRegister.setOnClickListener {
            val email: String = etEmail.text.toString().trim()
            val password: String = etPassword.text.toString().trim()
            val investorName: String = etInvestorName.text.toString().trim()
            val investmentCapacity: String = etInvestmentCapacity.text.toString().trim()
            val mentoringAvailable: Boolean? = when (rgMentoring.checkedRadioButtonId) {
                R.id.rbMentoringYes -> true
                R.id.rbMentoringNo -> false
                else -> null
            }
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
            if (investorName.isEmpty()) {
                etInvestorName.error = "Investor name is required"
                return@setOnClickListener
            }
            if (investmentCapacity.isEmpty()) {
                etInvestmentCapacity.error = "Investment capacity is required"
                return@setOnClickListener
            }
            if (mentoringAvailable == null) {
                Toast.makeText(this, "Please select if you offer mentoring", Toast.LENGTH_SHORT).show()
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

                    // Step 2: Save investor profile to Firestore
                    val investor: HashMap<String, Any> = hashMapOf(
                        "email" to email,
                        "oname" to investorName,
                        "name" to investorName.lowercase(),
                        "investmentCapacity" to (investmentCapacity.toDoubleOrNull() ?: 0.0),
                        "mentoringAvailable" to mentoringAvailable,
                        "preferredIndustry" to industry,
                        "timestamp" to System.currentTimeMillis(),
                        "userId" to userId,
                        "userType" to "investors"
                    )

                    db.collection("investors")
                        .document(userId) // Use userId as document ID for easier lookup
                        .set(investor)
                        .addOnSuccessListener {
                            btnRegister.isEnabled = true
                            Toast.makeText(
                                this,
                                "Registration successful!",
                                Toast.LENGTH_LONG
                            ).show()
                            clearForm(etEmail, etPassword, etInvestorName, etInvestmentCapacity, rgMentoring, spIndustry)
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
        etInvestorName: EditText,
        etInvestmentCapacity: EditText,
        rgMentoring: RadioGroup,
        spIndustry: Spinner
    ) {
        etEmail.text.clear()
        etPassword.text.clear()
        etInvestorName.text.clear()
        etInvestmentCapacity.text.clear()
        rgMentoring.clearCheck()
        spIndustry.setSelection(0)
    }
}