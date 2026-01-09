package com.example.smart_chat.activities.group_chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.fragment.GroupSettingsFragment
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager

class GroupChatSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_settings)

        val groupID = intent.getStringExtra("groupID")

        if (groupID == null) {
            finish()
            return
        }

        // Load fragment only if not already added (to handle screen rotation)
        if (savedInstanceState == null) {
            val fragment = GroupSettingsFragment.newInstance(groupID)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}