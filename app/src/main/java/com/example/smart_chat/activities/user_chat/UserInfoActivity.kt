package com.example.smart_chat.activities.user_chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.fragment.UserChatSettingsFragment
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.others.androidUtils

class UserInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        // Get user data from intent
        val otherUser = androidUtils.getUserModelFromIntent(intent)
        
        if (otherUser != null && savedInstanceState == null) {
            // Load UserChatSettingsFragment
            val fragment = UserChatSettingsFragment.newInstance(otherUser.userID!!)
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}
