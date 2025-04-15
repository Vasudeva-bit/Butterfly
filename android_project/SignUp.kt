package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // UI Elements
        val btnInvestor: Button = findViewById(R.id.btnInvestor)
        val btnEntrepreneur: Button = findViewById(R.id.btnEntrepreneur)
        val btnStartup: Button = findViewById(R.id.btnStartup)
        val btnMentor: Button = findViewById(R.id.btnMentor)

        // Entrepreneurs Button Click Listener
        btnEntrepreneur.setOnClickListener {
            val intent = Intent(this, EnRegister::class.java)
            startActivity(intent)
        }

        // Startups Button Click Listener
        btnStartup.setOnClickListener {
            val intent = Intent(this, StartupRegister::class.java)
            startActivity(intent)
        }

        // Investors Button Click Listener
        btnInvestor.setOnClickListener {
            val intent = Intent(this, InvestorRegister::class.java)
            startActivity(intent)
        }

        // Mentors Button Click Listener
        btnMentor.setOnClickListener {
            val intent = Intent(this, MentorRegister::class.java)
            startActivity(intent)
        }
    }
}
