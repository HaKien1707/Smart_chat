package com.example.smart_chat.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.others.androidUtils
import com.google.android.material.tabs.TabLayout

class UserChatSettingsFragment : Fragment() {

    private lateinit var backBtn: ImageButton
    private lateinit var moreBtn: ImageButton
    private lateinit var userProfileImage: ImageView
    private lateinit var userName: TextView
    private lateinit var userStatus: TextView
    private lateinit var usernameText: TextView
    private lateinit var messageBtn: LinearLayout
    private lateinit var muteBtn: LinearLayout
    private lateinit var callBtn: LinearLayout
    private lateinit var muteIcon: ImageView
    private lateinit var addToContactsBtn: LinearLayout
    private lateinit var tabs: TabLayout
    private lateinit var mediaRecycler: RecyclerView
    private lateinit var emptyText: TextView

    private var userID: String? = null
    private var user: userModel? = null
    private var isMuted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userID = it.getString("userID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_chat_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (userID == null) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            activity?.finish()
            return
        }

        initViews(view)
        setupListeners()
        setupTabs()
        setupRecycler()
        loadUserDetails()
    }

    private fun initViews(view: View) {
        backBtn = view.findViewById(R.id.back_btn)
        moreBtn = view.findViewById(R.id.more_btn)
        userProfileImage = view.findViewById(R.id.user_profile_image)
        userName = view.findViewById(R.id.user_name)
        userStatus = view.findViewById(R.id.user_status)
        usernameText = view.findViewById(R.id.username_text)
        messageBtn = view.findViewById(R.id.message_btn)
        muteBtn = view.findViewById(R.id.mute_btn)
        callBtn = view.findViewById(R.id.call_btn)
        muteIcon = view.findViewById(R.id.mute_icon)
        addToContactsBtn = view.findViewById(R.id.add_to_contacts_btn)
        tabs = view.findViewById(R.id.tabs)
        mediaRecycler = view.findViewById(R.id.media_recycler)
        emptyText = view.findViewById(R.id.empty_text)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            activity?.finish()
        }

        moreBtn.setOnClickListener {
            showMoreOptionsMenu()
        }

        messageBtn.setOnClickListener {
            // Go back to chat
            activity?.finish()
        }

        muteBtn.setOnClickListener {
            toggleMute()
        }

        callBtn.setOnClickListener {
            // TODO: Implement call functionality
            Toast.makeText(requireContext(), "Call ${user?.username}", Toast.LENGTH_SHORT).show()
        }

        addToContactsBtn.setOnClickListener {
            // TODO: Implement add to contacts
            Toast.makeText(requireContext(), "Add to contacts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        tabs.addTab(tabs.newTab().setText("Media"))
        tabs.addTab(tabs.newTab().setText("Links"))
        tabs.addTab(tabs.newTab().setText("Voice"))
        tabs.addTab(tabs.newTab().setText("Files"))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Show media
                        loadSharedMedia()
                    }
                    1 -> {
                        // Show links
                        showEmptyState("No shared links yet")
                    }
                    2 -> {
                        // Show voice
                        showEmptyState("No shared voice messages yet")
                    }
                    3 -> {
                        // Show files
                        showEmptyState("No shared files yet")
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecycler() {
        mediaRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        // TODO: Set up adapter for media grid
    }

    private fun loadUserDetails() {
        FirebaseAuthentication.allUsersCollection().document(userID!!).get()
            .addOnSuccessListener { document ->
                user = document.toObject(userModel::class.java)

                userName.text = user?.username ?: "Unknown User"
                usernameText.text = "@${user?.username?.lowercase()?.replace(" ", "") ?: "username"}"

                // Load user profile image
                val imageUrl = user?.profileImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(requireContext(), imageUrl, userProfileImage)
                } else {
                    userProfileImage.setImageResource(R.drawable.ic_profile)
                }

                // Update status
                updateUserStatus()
            }
            .addOnFailureListener { e ->
                Log.e("UserChatSettings", "Failed to load user", e)
                Toast.makeText(requireContext(), "Failed to load user details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserStatus() {
        // TODO: Implement real-time status update
        // For now, show a placeholder
        userStatus.text = "last seen recently"
    }

    private fun toggleMute() {
        isMuted = !isMuted
        if (isMuted) {
            muteIcon.setImageResource(R.drawable.ic_notifications_off)
            Toast.makeText(requireContext(), "Notifications muted", Toast.LENGTH_SHORT).show()
            // TODO: Save mute preference
        } else {
            muteIcon.setImageResource(R.drawable.ic_bell)
            Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
            // TODO: Remove mute preference
        }
    }

    private fun loadSharedMedia() {
        // TODO: Load shared media from Firebase
        // For now, show empty state
        showEmptyState("No shared media yet")
    }

    private fun showEmptyState(message: String) {
        mediaRecycler.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyText.text = message
    }

    private fun showMoreOptionsMenu() {
        val popupMenu = android.widget.PopupMenu(requireContext(), moreBtn)
        popupMenu.menuInflater.inflate(R.menu.menu_user_chat_settings, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_block_user -> {
                    showBlockUserDialog()
                    true
                }
                R.id.action_report_user -> {
                    showReportUserDialog()
                    true
                }
                R.id.action_delete_chat -> {
                    showDeleteChatDialog()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }

    private fun showBlockUserDialog() {
        // TODO: Show confirmation dialog
        Toast.makeText(requireContext(), "Block user dialog", Toast.LENGTH_SHORT).show()
    }

    private fun showReportUserDialog() {
        // TODO: Show report dialog
        Toast.makeText(requireContext(), "Report user dialog", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteChatDialog() {
        // TODO: Show confirmation dialog
        Toast.makeText(requireContext(), "Delete chat dialog", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(userID: String): UserChatSettingsFragment {
            return UserChatSettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("userID", userID)
                }
            }
        }
    }
}
