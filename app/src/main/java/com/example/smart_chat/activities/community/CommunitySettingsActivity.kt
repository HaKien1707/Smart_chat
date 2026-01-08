package com.example.smart_chat.activities.community

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.fragment.CommunitySettingsFragment
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager

class CommunitySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_settings)

        val communityID = intent.getStringExtra("communityID")

        if (communityID == null) {
            finish()
            return
        }

        // Load fragment only if not already added (to handle screen rotation)
        if (savedInstanceState == null) {
            val fragment = CommunitySettingsFragment.newInstance(communityID)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}
