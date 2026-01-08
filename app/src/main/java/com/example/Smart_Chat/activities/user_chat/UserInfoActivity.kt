package com.example.Smart_Chat.activities.user_chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.fragment.UserChatSettingsFragment
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.others.androidUtils

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
