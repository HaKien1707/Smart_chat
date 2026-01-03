package com.example.Smart_Chat.activities.user_chat

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.others.androidUtils

class UserInfoActivity : AppCompatActivity() {

    private var otherUser: userModel? = null

    private lateinit var backBtn: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView
    private lateinit var lastSeen: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        otherUser = androidUtils.getUserModelFromIntent(intent)

        initViews()
        setupUI()
    }

    private fun initViews() {
        backBtn = findViewById(R.id.back_btn)
        profileImage = findViewById(R.id.profile_image)
        userName = findViewById(R.id.user_name)
        lastSeen = findViewById(R.id.last_seen)

        backBtn.setOnClickListener { finish() }
    }

    private fun setupUI() {
        if (otherUser != null) {
            userName.text = otherUser!!.username
            // TODO: Set last seen status

            if (!otherUser!!.profileImage.isNullOrEmpty()) {
                androidUtils.setProfileImageFromBase64(
                    this,
                    otherUser!!.profileImage!!,
                    profileImage
                )
            }
        }
    }
}
