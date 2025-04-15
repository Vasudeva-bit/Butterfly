package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EnRegister : AppCompatActivity() {

    // Firebase instances
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_en_register)

        // UI Elements with explicit types
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val etCompanyName: EditText = findViewById(R.id.etCompanyName)
        val etFunding: EditText = findViewById(R.id.etFunding)
        val rgMentor: RadioGroup = findViewById(R.id.rgMentor)
        val rgCollab: RadioGroup = findViewById(R.id.rgCollab)
        val spInvestorType: Spinner = findViewById(R.id.spInvestorType)
        val btnRegister: Button = findViewById(R.id.btnRegister)

        // Register Button Click Listener
        btnRegister.setOnClickListener {
            val email: String = etEmail.text.toString().trim()
            val password: String = etPassword.text.toString().trim()
            val companyName: String = etCompanyName.text.toString().trim()
            val funding: String = etFunding.text.toString().trim()
            val mentorNeeded: Boolean? = when (rgMentor.checkedRadioButtonId) {
                R.id.rbMentorYes -> true
                R.id.rbMentorNo -> false
                else -> null
            }
            val collabNeeded: Boolean? = when (rgCollab.checkedRadioButtonId) {
                R.id.rbCollabYes -> true
                R.id.rbCollabNo -> false
                else -> null
            }
            val investorType: String = spInvestorType.selectedItem.toString()

            // Validation
            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (companyName.isEmpty()) {
                etCompanyName.error = "Company or idea name is required"
                return@setOnClickListener
            }
            if (funding.isEmpty()) {
                etFunding.error = "Funding amount is required"
                return@setOnClickListener
            }
            if (mentorNeeded == null) {
                Toast.makeText(this, "Please select if you need a mentor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (collabNeeded == null) {
                Toast.makeText(this, "Please select if you want collaboration", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (investorType == "Select Investor Type") {
                Toast.makeText(this, "Please select an investor type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show progress and disable button
            btnRegister.isEnabled = false

            // Step 1: Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user?.uid ?: return@addOnSuccessListener

                    // Step 2: Save entrepreneur profile to Firestore
                    val entrepreneur: HashMap<String, Any> = hashMapOf(
                        "email" to email,
                        "oname" to companyName,
                        "name" to companyName.lowercase(),
                        "fundingRequired" to (funding.toDoubleOrNull() ?: 0.0),
                        "mentorNeeded" to mentorNeeded,
                        "collabNeeded" to collabNeeded,
                        "investorType" to investorType,
                        "timestamp" to System.currentTimeMillis(),
                        "userId" to userId,
                        "userType" to "entrepreneurs"
                    )

                    db.collection("entrepreneurs")
                        .document(userId) // Use userId as document ID for easier lookup
                        .set(entrepreneur)
                        .addOnSuccessListener {
                            btnRegister.isEnabled = true
                            Toast.makeText(
                                this,
                                "Registration successful!",
                                Toast.LENGTH_LONG
                            ).show()
                            clearForm(etEmail, etPassword, etCompanyName, etFunding, rgMentor, rgCollab, spInvestorType)
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
        etCompanyName: EditText,
        etFunding: EditText,
        rgMentor: RadioGroup,
        rgCollab: RadioGroup,
        spInvestorType: Spinner
    ) {
        etEmail.text.clear()
        etPassword.text.clear()
        etCompanyName.text.clear()
        etFunding.text.clear()
        rgMentor.clearCheck()
        rgCollab.clearCheck()
        spInvestorType.setSelection(0)
    }
}