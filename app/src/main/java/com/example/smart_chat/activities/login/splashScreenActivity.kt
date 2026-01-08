package com.example.smart_chat.activities.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.activities.MainActivity
import com.example.smart_chat.utils.firebase.FirebaseAuthentication

class splashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            if (FirebaseAuthentication.isLoggedIn) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
            }
            finish()
        }, 2000)
    }
}