package com.example.smart_chat.activities

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.smart_chat.R
import com.example.smart_chat.databinding.ActivityMainBinding
import com.example.smart_chat.fragment.BlockedUsersFragment
import com.example.smart_chat.fragment.ChatFragment
import com.example.smart_chat.fragment.GroupFragment
import com.example.smart_chat.fragment.ProfileFragment
import com.example.smart_chat.fragment.FriendsListFragment
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.others.androidUtils
import com.google.android.material.navigation.NavigationView
import com.example.smart_chat.fragment.CommunityFragment
import com.example.smart_chat.fragment.NotificationFragment
import com.example.smart_chat.fragment.SearchUserFragment
import com.example.smart_chat.fragment.SettingsFragment
import com.example.smart_chat.fragment.TemporaryChatFragment
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.*
import com.google.firebase.firestore.ListenerRegistration
import android.Manifest
import com.example.smart_chat.models.video_call.VideoCallModel
import com.example.smart_chat.activities.login.WelcomeActivity

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    private lateinit var binding: ActivityMainBinding

    private var incomingCallListener: ListenerRegistration? = null
    private var unreadNotificationsListener: ListenerRegistration? = null

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

        // Keep header/bottom-nav state consistent even if fragments pop the back stack directly.
        supportFragmentManager.addOnBackStackChangedListener {
            syncUiWithCurrentFragment()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                        syncUiWithCurrentFragment() // Restore correct UI for current fragment
                    } else {
                        finish()
                    }
                }
            }
        })

        setupMainUI()
        // Theme changes can recreate the activity while keeping the current fragment.
        // Ensure header/bottom-nav visibility matches the current fragment (e.g., Settings).
        syncUiWithCurrentFragment()

        startListeningForIncomingCalls()
        startListeningForUnreadNotifications()
        getFCMtoken()
        loadUserDataIntoDrawer()
    }

    private fun startListeningForUnreadNotifications() {
        val userId = FirebaseAuthentication.currentUserID() ?: return

        // Remove old listener if any
        unreadNotificationsListener?.remove()

        unreadNotificationsListener = FirebaseNotifications.notificationsCollection()
            .whereEqualTo("recipientID", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                val count = snapshot?.size() ?: 0
                runOnUiThread {
                    if (count <= 0) {
                        binding.bottomNavigation.removeBadge(R.id.menu_notifications)
                    } else {
                        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.menu_notifications)
                        // Show exact unread count (avoid 9+/0+ style)
                        badge.maxCharacterCount = 4
                        badge.number = count
                        badge.isVisible = true
                    }
                }
            }
    }

    private fun syncUiWithCurrentFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_frame)

        when (currentFragment) {
            null -> setupMainUI()

            is SearchUserFragment -> {
                binding.bottomNavigation.visibility = View.GONE
                binding.searchBtn.visibility = View.GONE
                binding.panel.visibility = View.GONE
            }

            is SettingsFragment -> setupDetailUI("Settings")
            is ProfileFragment -> setupDetailUI("Profile")
            is FriendsListFragment -> setupDetailUI("Friends")
            is BlockedUsersFragment -> setupDetailUI("Blocked Users")

            else -> setupMainUI()
        }
    }

    private fun clearDrawerCheckedItem() {
        val menu = binding.navView.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            item.isChecked = false
            val subMenu = item.subMenu
            if (subMenu != null) {
                for (j in 0 until subMenu.size()) {
                    subMenu.getItem(j).isChecked = false
                }
            }
        }
    }

    private fun setupMainUI() {
        // We're back on a main screen; drawer should not keep highlighting the last selected item.
        clearDrawerCheckedItem()

        binding.panel.visibility = View.VISIBLE
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.searchBtn.visibility = View.VISIBLE

        binding.menuBtn.setImageResource(R.drawable.ic_options)
        binding.menuBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set title based on current bottom nav selection
        val currentItemId = binding.bottomNavigation.selectedItemId
        updateTitle(currentItemId)

        binding.searchBtn.setOnClickListener {
            // Hide main UI elements, but don't show title - SearchUserFragment has its own header
            binding.bottomNavigation.visibility = View.GONE
            binding.searchBtn.visibility = View.GONE
            binding.panel.visibility = View.GONE
            replaceFragment(SearchUserFragment(), true)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            updateTitle(item.itemId)
            when (item.itemId) {
                R.id.menu_chat -> {
                    clearBackStack()
                    replaceFragment(ChatFragment())
                }
                R.id.menu_temporary_chat -> {
                    clearBackStack()
                    replaceFragment(TemporaryChatFragment())
                }
                R.id.menu_notifications -> {
                    clearBackStack()
                    replaceFragment(NotificationFragment())
                }
            }
            true
        }
        
        binding.navView.setNavigationItemSelectedListener(this) // Add this line back
    }

    fun setupDetailUI(title: String) {
        binding.panel.visibility = View.VISIBLE
        binding.bottomNavigation.visibility = View.GONE
        binding.searchBtn.visibility = View.GONE

        binding.headerTitle.text = title
        binding.menuBtn.setImageResource(R.drawable.ic_arrow_back_24)
        binding.menuBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun updateTitle(itemId: Int) {
        val title = when (itemId) {
            R.id.menu_temporary_chat -> "Temporary Chat"
            R.id.menu_notifications -> getString(R.string.notification)
            else -> getString(R.string.title) // Default to app name
        }
        binding.headerTitle.text = title
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Notification permission is required for chat notifications", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startListeningForIncomingCalls() {
        try {
            incomingCallListener = FirebaseVideoCalls.listenForIncomingCalls { call ->
                showIncomingCall(call)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to listen for calls", e)
        }
    }

    private fun showIncomingCall(call: VideoCallModel) {
        FirebaseAuthentication.allUsersCollection().document(call.callerId ?: "").get()
            .addOnSuccessListener { doc ->
                val caller = doc.toObject(userModel::class.java)
                val intent = Intent(this, com.example.smart_chat.activities.video_call.IncomingCallActivity::class.java).apply {
                    putExtra("callId", call.callId)
                    putExtra("callerId", call.callerId)
                    putExtra("callerName", caller?.username)
                    putExtra("callerImage", caller?.profileImage)
                    putExtra("callType", call.type)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
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
                    androidUtils.setProfileImageFromBase64(this, user.profileImage, imageView)
                }
            }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                setupDetailUI("Profile")
                replaceFragment(ProfileFragment(), true)
            }
            R.id.nav_contacts -> {
                setupDetailUI("Friends")
                replaceFragment(FriendsListFragment(), true)
            }
            R.id.nav_settings -> {
                setupDetailUI("Settings")
                replaceFragment(SettingsFragment(), true)
            }
            R.id.nav_logout -> {
                logoutUser()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        // Show confirmation dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_msg)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                // Perform logout
                performLogout()
            }
            .setNegativeButton(android.R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        
        // Set button text color to white
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.WHITE)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.WHITE)
    }

    private fun performLogout() {
        try {
            // Stop listening for incoming calls
            incomingCallListener?.remove()

            // Sign out from Firebase
            FirebaseAuthentication.logout()

            // Clear app data/cache if needed
            // Optional: Clear SharedPreferences or other local data
            val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
            sharedPref?.edit()?.clear()?.apply()

            // Navigate to Welcome Activity
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_frame, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun clearBackStack() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun getFCMtoken() {
        // FCM token logic...
    }

    private fun createNotificationChannel() {
        // Notification channel logic...
    }

    override fun onDestroy() {
        unreadNotificationsListener?.remove()
        incomingCallListener?.remove()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        binding.navView.setCheckedItem(View.NO_ID)
    }
}