package com.example.smart_chat.activities.community

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.fragment.CommunityChatFragment
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager

class CommunityChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_chat)

        // Get communityID from Intent
        val communityID = intent.getStringExtra("communityID")

        if (communityID.isNullOrEmpty()) {
            Log.e("CommunityChatActivity", "communityID is null or empty!")
            finish()
            return
        }

        // Load CommunityChatFragment if not already loaded
        if (savedInstanceState == null) {
            val fragment = CommunityChatFragment.newInstance(communityID)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}