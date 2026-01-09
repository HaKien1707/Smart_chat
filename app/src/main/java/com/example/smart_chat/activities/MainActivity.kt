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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.smart_chat.R
import com.example.smart_chat.databinding.ActivityMainBinding
import com.example.smart_chat.fragment.ChatFragment
import com.example.smart_chat.fragment.GroupFragment
import com.example.smart_chat.fragment.ProfileFragment
import com.example.smart_chat.fragment.FriendsListFragment
import com.example.smart_chat.fragment.DeletedChatsFragment
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

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    private lateinit var binding: ActivityMainBinding

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
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                        setupMainUI() // Restore main UI when coming back
                    } else {
                        finish()
                    }
                }
            }
        })

        setupMainUI()

        startListeningForIncomingCalls()
        getFCMtoken()
        loadUserDataIntoDrawer()
    }

    private fun setupMainUI() {
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
                R.id.menu_chat -> replaceFragment(ChatFragment())
                R.id.menu_temporary_chat -> replaceFragment(TemporaryChatFragment())
                R.id.menu_notifications -> replaceFragment(NotificationFragment())
                R.id.menu_community -> replaceFragment(CommunityFragment())
                R.id.menu_group -> replaceFragment(GroupFragment())
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
            R.id.menu_community -> "Community"
            R.id.menu_group -> getString(R.string.menu_group)
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
                setupDetailUI("Contacts")
                replaceFragment(FriendsListFragment(), true)
            }
            R.id.nav_settings -> {
                setupDetailUI("Settings")
                replaceFragment(SettingsFragment(), true)
            }
            R.id.nav_deleted_chats -> {
                setupDetailUI("Deleted Chats")
                replaceFragment(DeletedChatsFragment(), true)
            }
            R.id.nav_logout -> {
                logoutUser()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        // Logout logic...
    }

    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_frame, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun getFCMtoken() {
        // FCM token logic...
    }

    private fun createNotificationChannel() {
        // Notification channel logic...
    }

    override fun onDestroy() {
        super.onDestroy()
        incomingCallListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        binding.navView.setCheckedItem(View.NO_ID)
    }
}