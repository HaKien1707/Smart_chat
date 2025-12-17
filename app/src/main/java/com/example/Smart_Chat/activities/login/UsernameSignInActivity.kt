package com.example.Smart_Chat.activities.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.MainActivity
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils

class UsernameSignInActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var continueBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username_sign_in)

        profileImage = findViewById(R.id.icon)
        usernameText = findViewById(R.id.usernameText)
        continueBtn = findViewById(R.id.login_username_BTN)

        loadUserData()

        continueBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserData() {
        FireBase_utils.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)

                usernameText.text = "Welcome, ${user?.username ?: "User"}!"

                if (!user?.profileImage.isNullOrEmpty()) {
                    androidUtils.setProfileImageFromBase64(
                        this,
                        user?.profileImage,
                        profileImage
                    )
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile)
                }
            }
    }
}