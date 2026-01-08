package com.example.smart_chat.activities.user_chat

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smart_chat.R
import com.example.smart_chat.fragment.UserChatFragment
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.others.androidUtils

class ChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.header_green)
        }

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
