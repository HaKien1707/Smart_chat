package com.example.Smart_Chat.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.Smart_Chat.R
import com.example.Smart_Chat.databinding.ActivityMainBinding
import com.example.Smart_Chat.fragment.ChatFragment
import com.example.Smart_Chat.fragment.GroupFragment
import com.example.Smart_Chat.fragment.ProfileFragment
import com.example.Smart_Chat.fragment.FriendsListFragment
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.others.androidUtils
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
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.firebase.*
import com.google.firebase.firestore.ListenerRegistration
import android.Manifest
import com.example.Smart_Chat.models.video_call.VideoCallModel

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    private lateinit var binding: ActivityMainBinding
    private var currentTab = "chat"
    private lateinit var notificationBadge: TextView

    // Real-time listeners
    private var friendRequestListener: ListenerRegistration? = null
    private var groupRequestListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    private var incomingCallListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()

        if (savedInstanceState == null) {
            replaceFragment(ChatFragment())
        }

        createNotificationChannel()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        })

        replaceFragment(ChatFragment())
        setupNavigationDrawer()

        notificationBadge = findViewById(R.id.notification_badge)

        binding.menuBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.notificationBtn.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        // Start real-time notification counting
        startNotificationListeners()

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

        startListeningForIncomingCalls()

        getFCMtoken()
        loadUserDataIntoDrawer()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("FCM", "Notification permission granted")
            } else {
                Log.w("FCM", "Notification permission denied")
                Toast.makeText(
                    this,
                    "Notification permission is required for chat notifications",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startListeningForIncomingCalls() {
        try {
            incomingCallListener = FirebaseVideoCalls.listenForIncomingCalls { call ->
                // Show incoming call screen
                showIncomingCall(call)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to listen for calls", e)
        }
    }

    private fun showIncomingCall(call: VideoCallModel) {
        // Get caller info
        FirebaseAuthentication.allUsersCollection().document(call.callerId ?: "").get()
            .addOnSuccessListener { doc ->
                val caller = doc.toObject(userModel::class.java)

                val intent = Intent(this, com.example.Smart_Chat.activities.video_call.IncomingCallActivity::class.java)
                intent.putExtra("callId", call.callId)
                intent.putExtra("callerId", call.callerId)
                intent.putExtra("callerName", caller?.username)
                intent.putExtra("callerImage", caller?.profileImage)
                intent.putExtra("callType", call.type)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
    }

    private fun startNotificationListeners() {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

        var friendRequestCount = 0
        var groupRequestCount = 0
        var infoNotificationCount = 0

        fun updateBadge() {
            val total = friendRequestCount + groupRequestCount + infoNotificationCount
            runOnUiThread {
                updateNotificationBadge(total)
            }
        }

        // Listen to friend requests
        friendRequestListener = FirebaseFriends.friendRequestsCollection()
            .whereEqualTo("receiverID", currentUserID)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("MainActivity", "Friend request listener error", error)
                    return@addSnapshotListener
                }

                friendRequestCount = snapshots?.size() ?: 0
                Log.d("MainActivity", "Friend requests: $friendRequestCount")
                updateBadge()
            }

        // Listen to group join requests (for groups where user is admin)
        FirebaseGroups.allGroupsCollection()
            .whereArrayContains("adminIDs", currentUserID)
            .get()
            .addOnSuccessListener { groupDocs ->
                val adminGroupIDs = groupDocs.mapNotNull { it.id }

                if (adminGroupIDs.isEmpty()) {
                    groupRequestCount = 0
                    updateBadge()
                    return@addOnSuccessListener
                }

                // Listen to all group join requests for groups where user is admin
                groupRequestListener = FirebaseGroups.groupJoinRequestsCollection()
                    .whereIn("groupID", adminGroupIDs)
                    .whereEqualTo("status", "pending")
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.e("MainActivity", "Group request listener error", error)
                            return@addSnapshotListener
                        }

                        groupRequestCount = snapshots?.size() ?: 0
                        Log.d("MainActivity", "Group requests: $groupRequestCount")
                        updateBadge()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to load admin groups", e)
            }

        // Listen to info notifications
        notificationListener = FirebaseNotifications.notificationsCollection()
            .whereEqualTo("recipientID", currentUserID)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("MainActivity", "Notification listener error", error)
                    return@addSnapshotListener
                }

                infoNotificationCount = snapshots?.size() ?: 0
                Log.d("MainActivity", "Info notifications: $infoNotificationCount")
                updateBadge()
            }
    }

    private var lastBadgeCount = 0

    private fun updateNotificationBadge(count: Int) {
        Log.d("MainActivity", "Total notification count: $count")

        if (count > 0) {
            notificationBadge.text = if (count > 99) "99+" else count.toString()
            notificationBadge.visibility = View.VISIBLE

            // Animate if count increased
            if (count > lastBadgeCount) {
                notificationBadge.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(200)
                    .withEndAction {
                        notificationBadge.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            }
        } else {
            notificationBadge.visibility = View.GONE
        }

        lastBadgeCount = count
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
        FirebaseAuthentication.currentUserDetails().get()
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
                        user.profileImage,
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
        return false
    }

    private fun deselectBottomNavigation() {
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)

        for (i in 0 until binding.bottomNavigation.menu.size) {
            binding.bottomNavigation.menu[i].isChecked = false
        }

        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

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

                        FirebaseAuthentication.logout()

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
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_frame, fragment)

        // Add to backstack unless it's the home fragment
        if (fragment !is ChatFragment) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    private fun getFCMtoken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "My Token: $token")

                FirebaseAuthentication.currentUserDetails().update("fcmToken", token)
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

    override fun onDestroy() {
        super.onDestroy()
        friendRequestListener?.remove()
        groupRequestListener?.remove()
        notificationListener?.remove()
        incomingCallListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        binding.navView.setCheckedItem(View.NO_ID)
    }
}