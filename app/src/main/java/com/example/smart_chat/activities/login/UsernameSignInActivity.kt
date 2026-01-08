package com.example.smart_chat.activities.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.activities.MainActivity
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.firebase.FirebaseAuthentication

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
        FirebaseAuthentication.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)

                usernameText.text = "Welcome, ${user?.username ?: "User"}!"

                if (!user?.profileImage.isNullOrEmpty()) {
                    androidUtils.setProfileImageFromBase64(
                        this,
                        user.profileImage,
                        profileImage
                    )
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile)
                }
            }
    }
}