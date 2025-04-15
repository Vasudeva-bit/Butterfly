package com.example.android_project

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType

class ChatBotActivity : AppCompatActivity() {
    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var inputText: EditText
    private lateinit var sendButton: Button
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_bot)

        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        inputText = findViewById(R.id.inputText)
        sendButton = findViewById(R.id.sendButton)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = currentUser.uid

        // Load existing chats from Firestore
        loadChatHistory(userId)

        sendButton.setOnClickListener {
            val userMessage = inputText.text.toString().trim()
            if (userMessage.isNotBlank()) {
                addChatMessage("You: $userMessage", true)
                callGeminiApi(userMessage) { botResponse ->
                    runOnUiThread {
                        addChatMessage("Bot: $botResponse", false)
                        saveChatToFirestore(userId, userMessage, botResponse)
                        inputText.text.clear()
                        scrollToBottom()
                    }
                }
            }
        }
    }

    private fun loadChatHistory(userId: String) {
        db.collection("chatbots")
            .document(userId)
            .collection("chats")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val userMessage = doc.getString("userMessage") ?: ""
                    val botResponse = doc.getString("botResponse") ?: ""
                    addChatMessage("You: $userMessage", true)
                    addChatMessage("Bot: $botResponse", false)
                }
                scrollToBottom()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading chat history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChatToFirestore(userId: String, userMessage: String, botResponse: String) {
        val chatData = hashMapOf(
            "userMessage" to userMessage,
            "botResponse" to botResponse,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chatbots")
            .document(userId)
            .collection("chats")
            .add(chatData)
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving chat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addChatMessage(message: String, isUser: Boolean) {
        val textView = TextView(this)
        textView.text = message
        textView.textSize = 16f
        textView.setTextColor(Color.BLACK)
        textView.setPadding(16, 8, 16, 8)

        textView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 4, 8, 4)
            gravity = if (isUser) Gravity.END else Gravity.START
        }
        textView.background = ContextCompat.getDrawable(this, R.drawable.rounded_background)
        textView.setBackgroundColor(if (isUser) Color.parseColor("#D1C4E9") else Color.parseColor("#C5CAE9"))

        chatContainer.addView(textView)
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun createGeminiRequestJson(prompt: String): String {
        val textObject = JSONObject().put("text", prompt)
        val partsArray = JSONArray().put(textObject)
        val contentObject = JSONObject().put("parts", partsArray)
        val contentsArray = JSONArray().put(contentObject)
        val requestObject = JSONObject().put("contents", contentsArray)

        return requestObject.toString()
    }

    private fun callGeminiApi(userMessage: String, callback: (String) -> Unit) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyC9j6aM6MIuo_strmUkbsXuJWAd3UXn0qk"
        val body = RequestBody.create("application/json".toMediaType(), createGeminiRequestJson(userMessage))

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback("Failed to get response")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (responseBody != null) {
                    val responseText = responseBody.string()
                    try {
                        val jsonResponse = JSONObject(responseText)
                        val candidatesArray = jsonResponse.getJSONArray("candidates")
                        if (candidatesArray.length() > 0) {
                            val firstCandidate = candidatesArray.getJSONObject(0)
                            val content = firstCandidate.getJSONObject("content")
                            val partsArray = content.getJSONArray("parts")
                            if (partsArray.length() > 0) {
                                val text = partsArray.getJSONObject(0).getString("text")
                                callback(text)
                            } else {
                                callback("No parts found")
                            }
                        } else {
                            callback("No candidates found")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback("Failed to parse response")
                    }
                } else {
                    callback("Empty response")
                }
            }
        })
    }
}