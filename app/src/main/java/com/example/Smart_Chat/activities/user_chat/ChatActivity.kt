package com.example.Smart_Chat.activities.user_chat

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.fragment.UserChatFragment
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.others.androidUtils

class ChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Get otherUser from Intent
        val otherUser = androidUtils.getUserModelFromIntent(intent)

        if (otherUser == null) {
            Log.e("ChatActivity", "otherUser is null!")
            Toast.makeText(this, "Error loading user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load UserChatFragment if not already loaded
        if (savedInstanceState == null) {
            val fragment = UserChatFragment.newInstance(otherUser)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}
