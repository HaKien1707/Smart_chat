package com.example.Smart_Chat.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.Smart_Chat.R
import com.example.Smart_Chat.databinding.ActivityMainBinding
import com.example.Smart_Chat.fragment.ChatFragment
import com.example.Smart_Chat.fragment.GroupFragment
import com.example.Smart_Chat.fragment.ProfileFragment
import com.example.Smart_Chat.fragment.FriendsListFragment
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging
import androidx.core.view.size
import androidx.core.view.get
import com.example.Smart_Chat.activities.community.CreateCommunityActivity
import com.example.Smart_Chat.activities.group_chat.SearchGroupActivity
import com.example.Smart_Chat.activities.login.splashScreenActivity
import com.example.Smart_Chat.activities.others.DeletedChatsActivity
import com.example.Smart_Chat.activities.others.NotificationActivity
import com.example.Smart_Chat.activities.temporary_chat.CreateTemporaryChatActivity
import com.example.Smart_Chat.activities.user_chat.SearchUserActivity
import com.example.Smart_Chat.fragment.CommunityFragment
import com.example.Smart_Chat.fragment.SettingsFragment
import com.example.Smart_Chat.fragment.TemporaryChatFragment
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private var currentTab = "chat" // Track current tab
    private lateinit var notificationBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme and language BEFORE super.onCreate()
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        })

        // Load default fragment
        replaceFragment(ChatFragment())

        // Set up Navigation Drawer
        setupNavigationDrawer()

        notificationBadge = findViewById(R.id.notification_badge)

        // Set up menu button to open drawer
        binding.menuBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set up notification button
        binding.notificationBtn.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        // Load notification count
        loadNotificationCount()

        // Set up FAB - DYNAMIC BASED ON TAB
        binding.fabSearchUser.setOnClickListener {
            when (currentTab) {
                "chat" -> {
                    val intent = Intent(this, SearchUserActivity::class.java)
                    startActivity(intent)
                }
                "group" -> {
                    val intent = Intent(this, SearchGroupActivity::class.java)
                    startActivity(intent)
                }
                "temporary_chat" -> {
                    val intent = Intent(this, CreateTemporaryChatActivity::class.java)
                    startActivity(intent)
                }
                "community" -> {
                    val intent = Intent(this, CreateCommunityActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        // Set up bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_chat -> {
                    currentTab = "chat"
                    updateFabIcon()
                    replaceFragment(ChatFragment())
                    true
                }
                R.id.menu_temporary_chat -> {
                    currentTab = "temporary_chat"
                    updateFabIcon()
                    replaceFragment(TemporaryChatFragment())
                    true
                }
                R.id.menu_community -> {
                    currentTab = "community"
                    updateFabIcon()
                    replaceFragment(CommunityFragment())
                    true
                }
                R.id.menu_group -> {
                    currentTab = "group"
                    updateFabIcon()
                    replaceFragment(GroupFragment())
                    true
                }
                else -> false
            }
        }

        getFCMtoken()
        loadUserDataIntoDrawer()
    }

    private fun updateFabIcon() {
        when (currentTab) {
            "chat" -> {
                binding.fabSearchUser.setImageResource(R.drawable.ic_search)
                binding.fabSearchUser.show()
            }
            "group" -> {
                binding.fabSearchUser.setImageResource(R.drawable.ic_search)
                binding.fabSearchUser.show()
            }
            "temporary_chat" -> {
                binding.fabSearchUser.setImageResource(R.drawable.ic_add)
                binding.fabSearchUser.show()
            }
            "community" -> {
                binding.fabSearchUser.setImageResource(R.drawable.ic_add)
                binding.fabSearchUser.show()
            }
            else -> {
                binding.fabSearchUser.hide()
            }
        }
    }

    private fun setupNavigationDrawer() {
        binding.navView.setNavigationItemSelectedListener(this)
    }

    private fun loadUserDataIntoDrawer() {
        FireBase_utils.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)

                val headerView = binding.navView.getHeaderView(0)
                val nameTextView = headerView.findViewById<TextView>(R.id.nav_header_name)
                val phoneTextView = headerView.findViewById<TextView>(R.id.nav_header_phone)
                val imageView = headerView.findViewById<ImageView>(R.id.nav_header_image)

                nameTextView.text = user?.username ?: "User"
                phoneTextView.text = user?.phoneNumber ?: ""

                if (!user?.profileImage.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(
                        this,
                        user?.profileImage,
                        imageView
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to load user data", e)
            }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                deselectBottomNavigation()
                replaceFragment(ProfileFragment())
            }
            R.id.nav_contacts -> {
                deselectBottomNavigation()
                replaceFragment(FriendsListFragment())
            }
            R.id.nav_settings -> {
                deselectBottomNavigation()
                replaceFragment(SettingsFragment())
            }
            R.id.nav_deleted_chats -> {
                val intent = Intent(this, DeletedChatsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                logoutUser()
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Helper function to deselect bottom navigation
    private fun deselectBottomNavigation() {
        // Temporarily disable group checkable behavior
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)

        // Uncheck all items
        for (i in 0 until binding.bottomNavigation.menu.size) {
            binding.bottomNavigation.menu[i].isChecked = false
        }

        // Re-enable group checkable behavior
        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        // Hide FAB and reset tab
        binding.fabSearchUser.hide()
        currentTab = "none"
    }

    private fun logoutUser() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseMessaging.getInstance().deleteToken()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FCM_TOKEN", "Token deleted successfully")
                        } else {
                            Log.e("FCM_TOKEN", "Failed to delete token", task.exception)
                        }

                        FireBase_utils.logout()

                        val intent = Intent(this, splashScreenActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commit()
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

    private fun loadNotificationCount() {
        var totalCount = 0
        var friendRequestsLoaded = false
        var groupRequestsLoaded = false

        // Count pending friend requests
        FireBase_utils.getPendingFriendRequests(
            onSuccess = { requests ->
                totalCount += requests.size
                friendRequestsLoaded = true
                if (friendRequestsLoaded && groupRequestsLoaded) {
                    updateNotificationBadge(totalCount)
                }
            },
            onFailure = { e ->
                Log.e("MainActivity", "Failed to load friend requests", e)
                friendRequestsLoaded = true
                if (friendRequestsLoaded && groupRequestsLoaded) {
                    updateNotificationBadge(totalCount)
                }
            }
        )

        // Count pending group join requests (if user is admin)
        FireBase_utils.getAllPendingGroupJoinRequestsForAdmin(
            onSuccess = { requests ->
                totalCount += requests.size
                groupRequestsLoaded = true
                if (friendRequestsLoaded && groupRequestsLoaded) {
                    updateNotificationBadge(totalCount)
                }
            },
            onFailure = { e ->
                Log.e("MainActivity", "Failed to load group requests", e)
                groupRequestsLoaded = true
                if (friendRequestsLoaded && groupRequestsLoaded) {
                    updateNotificationBadge(totalCount)
                }
            }
        )
    }

    private fun updateNotificationBadge(count: Int) {
        runOnUiThread {  // Ensure UI update on main thread
            if (count > 0) {
                notificationBadge.text = if (count > 99) "99+" else count.toString()
                notificationBadge.visibility = View.VISIBLE
            } else {
                notificationBadge.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh notification count when returning to MainActivity
        loadNotificationCount()
    }
}