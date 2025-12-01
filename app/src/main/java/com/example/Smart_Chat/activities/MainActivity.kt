package com.example.Smart_Chat.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.fragment.ChatFragment
import com.example.Smart_Chat.fragment.GroupFragment
import com.example.Smart_Chat.fragment.ProfileFragment
import com.example.Smart_Chat.utils.FireBase_utils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var searchBTN: ImageButton
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var chatFragment: ChatFragment
    private lateinit var groupFragment: GroupFragment
    private lateinit var profileFragment: ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        searchBTN = findViewById(R.id.main_search_btn)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        chatFragment = ChatFragment()
        groupFragment = GroupFragment()
        profileFragment = ProfileFragment()

        searchBTN.setOnClickListener {
            val intent = Intent(this, SearchUserActivity::class.java)
            startActivity(intent)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_chat -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frame, chatFragment)
                        .commit()
                }
                R.id.menu_group -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frame, groupFragment)
                        .commit()
                }
                R.id.menu_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frame, profileFragment)
                        .commit()
                }
            }
            true
        }

        bottomNavigationView.selectedItemId = R.id.menu_chat

        getFCMtoken()
    }

    private fun getFCMtoken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "My Token: $token")

                FireBase_utils.currentUserDetails().update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM_TOKEN", "Token saved to Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM_TOKEN", "Failed to save token: ${e.message}")
                    }
            } else {
                Log.e("FCM_TOKEN", "Failed to get token", task.exception)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "chat_notifications"
            val name: CharSequence = "Chat Notifications"
            val description = "Notifications for new chat messages"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, name, importance).apply {
                setDescription(description)
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            Log.d("FCM", "Notification channel created")
        }
    }
}