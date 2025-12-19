package com.example.Smart_Chat.activities.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.MainActivity
import com.example.Smart_Chat.activities.user_chat.ChatActivity
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.androidUtils
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication

class splashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        if (FirebaseAuthentication.isLoggedIn && intent.extras != null) {
            val userID = intent.extras?.getString("userID")

            userID?.let {
                FirebaseAuthentication.allUsersCollection().document(it).get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val model = task.result.toObject(userModel::class.java)

                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            startActivity(mainIntent)

                            val chatIntent = Intent(this, ChatActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            androidUtils.passUserModelAsIntent(chatIntent, model)
                            startActivity(chatIntent)
                        }
                    }
            }
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this@splashScreenActivity, WelcomeActivity::class.java)
                startActivity(intent)
                finish()
            }, 2000)
        }
    }
}