package com.example.smart_chat.activities.others

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.fragment.DeletedChatsFragment
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager

class DeletedChatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deleted_chats)

        findViewById<android.widget.ImageButton>(R.id.back_btn).setOnClickListener {
            finish()
        }

        // Load DeletedChatsFragment if not already loaded
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DeletedChatsFragment())
                .commit()
        }
    }
}