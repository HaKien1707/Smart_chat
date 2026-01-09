package com.example.smart_chat.activities.group_chat

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.fragment.GroupChatFragment
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager

class GroupChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        // Get groupID from Intent
        val groupID = intent.getStringExtra("groupID")

        if (groupID.isNullOrEmpty()) {
            Log.e("GroupChatActivity", "groupID is null or empty!")
            finish()
            return
        }

        // Load GroupChatFragment if not already loaded
        if (savedInstanceState == null) {
            val fragment = GroupChatFragment.newInstance(groupID)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}