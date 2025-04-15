package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StartupRegister : AppCompatActivity() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup_register)

        // UI Elements
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val etStartupName: EditText = findViewById(R.id.etStartupName)
        val etDescription: EditText = findViewById(R.id.etDescription)
        val etFundingGoal: EditText = findViewById(R.id.etFundingGoal)
        val spIndustry: Spinner = findViewById(R.id.spIndustry)
        val rgOpenToCollab: RadioGroup = findViewById(R.id.rgOpenToCollab)
        val btnRegister: Button = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)

        // Register Button Click Listener
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val startupName = etStartupName.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val fundingGoal = etFundingGoal.text.toString().trim()
            val industry = spIndustry.selectedItem.toString()
            val openToCollab = when (rgOpenToCollab.checkedRadioButtonId) {
                R.id.rbCollabYes -> true
                R.id.rbCollabNo -> false
                else -> null
            }

            // Validation
            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (startupName.isEmpty()) {
                etStartupName.error = "Startup name is required"
                return@setOnClickListener
            }
            if (description.isEmpty()) {
                etDescription.error = "Description is required"
                return@setOnClickListener
            }
            if (fundingGoal.isEmpty()) {
                etFundingGoal.error = "Funding goal is required"
                return@setOnClickListener
            }
            if (industry == "Select Industry") {
                Toast.makeText(this, "Please select an industry", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (openToCollab == null) {
                Toast.makeText(this, "Please select collaboration preference", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show progress and disable button
            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            // Step 1: Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user?.uid ?: return@addOnSuccessListener

                    // Step 2: Save startup data to Firestore
                    val startupData: HashMap<String, Any> = hashMapOf(
                        "email" to email,
                        "oname" to startupName,
                        "name" to startupName.lowercase(),
                        "description" to description,
                        "fundingGoal" to (fundingGoal.toDoubleOrNull() ?: 0.0),
                        "industry" to industry,
                        "openToCollab" to openToCollab,
                        "timestamp" to System.currentTimeMillis(),
                        "userId" to userId,
                        "userType" to "startups"
                    )

                    db.collection("startups")
                        .document(userId) // Use userId as document ID
                        .set(startupData)
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            btnRegister.isEnabled = true
                            Toast.makeText(
                                this,
                                "Startup registered successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            clearForm(etEmail, etPassword, etStartupName, etDescription, etFundingGoal, spIndustry, rgOpenToCollab)
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            progressBar.visibility = View.GONE
                            btnRegister.isEnabled = true
                            Toast.makeText(
                                this,
                                "Error saving startup: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { exception ->
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    Toast.makeText(
                        this,
                        "Error creating account: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun clearForm(
        etEmail: EditText,
        etPassword: EditText,
        etStartupName: EditText,
        etDescription: EditText,
        etFundingGoal: EditText,
        spIndustry: Spinner,
        rgOpenToCollab: RadioGroup
    ) {
        etEmail.text.clear()
        etPassword.text.clear()
        etStartupName.text.clear()
        etDescription.text.clear()
        etFundingGoal.text.clear()
        spIndustry.setSelection(0)
        rgOpenToCollab.clearCheck()
    }
}