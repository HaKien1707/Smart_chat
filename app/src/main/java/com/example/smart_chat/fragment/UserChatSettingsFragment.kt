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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.shared.SharedFilesAdapter
import com.example.smart_chat.adapters.shared.SharedLinksAdapter
import com.example.smart_chat.adapters.shared.SharedMediaAdapter
import com.example.smart_chat.models.userModel
import com.example.smart_chat.models.MsgModel
import com.example.smart_chat.models.shared.SharedFileItem
import com.example.smart_chat.models.shared.SharedLinkItem
import com.example.smart_chat.models.shared.SharedMediaItem
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseBlocking
import com.example.smart_chat.utils.firebase.FirebaseChat
import com.example.smart_chat.utils.firebase.FirebaseFriends
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.shared.SharedContentClassifier
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.Query

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
    private lateinit var addToContactsIcon: ImageView
    private lateinit var addToContactsText: TextView
    private lateinit var tabs: TabLayout
    private lateinit var mediaRecycler: RecyclerView
    private lateinit var emptyText: TextView

    private lateinit var mediaAdapter: SharedMediaAdapter
    private lateinit var linksAdapter: SharedLinksAdapter
    private lateinit var filesAdapter: SharedFilesAdapter

    private var sharedLoaded: Boolean = false
    private val sharedMediaItems = mutableListOf<SharedMediaItem>()
    private val sharedLinkItems = mutableListOf<SharedLinkItem>()
    private val sharedFileItems = mutableListOf<SharedFileItem>()

    private var userID: String? = null
    private var user: userModel? = null
    private var isMuted: Boolean = false
    private var muteUntil: Long? = null
    private var friendshipStatus: FirebaseFriends.FriendshipStatus = FirebaseFriends.FriendshipStatus.NOT_FRIENDS

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
        loadSharedContent()
        loadMuteState()
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
        addToContactsIcon = view.findViewById(R.id.add_to_contacts_icon)
        addToContactsText = view.findViewById(R.id.add_to_contacts_text)
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
            onMuteClicked()
        }

        callBtn.setOnClickListener {
            val currentUser = user
            if (currentUser?.userID.isNullOrBlank()) {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fragment = CallFragment.newInstance(
                receiverId = currentUser!!.userID!!,
                receiverName = currentUser.username,
                receiverImage = currentUser.profileImage,
                callType = "video"
            )

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        addToContactsBtn.setOnClickListener {
            val targetUserId = userID
            if (targetUserId.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.noUserMatch), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (friendshipStatus) {
                FirebaseFriends.FriendshipStatus.FRIENDS -> {
                    val displayName = user?.username ?: "this user"
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.remove_from_contacts))
                        .setMessage(getString(R.string.unfriend_confirm_message, displayName))
                        .setPositiveButton(getString(R.string.remove_from_contacts)) { _, _ ->
                            FirebaseFriends.removeFriend(
                                friendID = targetUserId,
                                onSuccess = {
                                    friendshipStatus = FirebaseFriends.FriendshipStatus.NOT_FRIENDS
                                    updateContactsButtonUI()
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.remove_from_contacts),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = {
                                    Toast.makeText(requireContext(), it.message ?: "Error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                }

                FirebaseFriends.FriendshipStatus.REQUEST_SENT -> {
                    FirebaseFriends.cancelFriendRequest(
                        receiverID = targetUserId,
                        onSuccess = {
                            friendshipStatus = FirebaseFriends.FriendshipStatus.NOT_FRIENDS
                            updateContactsButtonUI()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.cancel_friend_request),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = {
                            Toast.makeText(requireContext(), it.message ?: "Error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                FirebaseFriends.FriendshipStatus.REQUEST_RECEIVED -> {
                    FirebaseFriends.acceptFriendRequest(
                        senderID = targetUserId,
                        onSuccess = {
                            friendshipStatus = FirebaseFriends.FriendshipStatus.FRIENDS
                            updateContactsButtonUI()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.accept_friend_request),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = {
                            Toast.makeText(requireContext(), it.message ?: "Error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                FirebaseFriends.FriendshipStatus.NOT_FRIENDS -> {
                    val receiverName = user?.username ?: ""
                    FirebaseFriends.sendFriendRequest(
                        receiverID = targetUserId,
                        receiverName = receiverName,
                        onSuccess = {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.add_to_contacts),
                                Toast.LENGTH_SHORT
                            ).show()
                            refreshFriendshipStatus()
                        },
                        onFailure = {
                            Toast.makeText(requireContext(), it.message ?: "Error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun refreshFriendshipStatus() {
        val targetUserId = userID ?: return
        FirebaseFriends.checkFriendshipStatus(targetUserId) {
            friendshipStatus = it
            updateContactsButtonUI()
        }
    }

    private fun updateContactsButtonUI() {
        if (!this::addToContactsText.isInitialized || !this::addToContactsIcon.isInitialized) return

        when (friendshipStatus) {
            FirebaseFriends.FriendshipStatus.FRIENDS -> {
                addToContactsText.setText(R.string.remove_from_contacts)
                addToContactsIcon.setImageResource(R.drawable.ic_person_remove)
            }
            FirebaseFriends.FriendshipStatus.REQUEST_SENT -> {
                addToContactsText.setText(R.string.cancel_friend_request)
                addToContactsIcon.setImageResource(R.drawable.ic_close)
            }
            FirebaseFriends.FriendshipStatus.REQUEST_RECEIVED -> {
                addToContactsText.setText(R.string.accept_friend_request)
                addToContactsIcon.setImageResource(R.drawable.ic_check)
            }
            FirebaseFriends.FriendshipStatus.NOT_FRIENDS -> {
                addToContactsText.setText(R.string.add_to_contacts)
                addToContactsIcon.setImageResource(R.drawable.ic_add_person)
            }
        }
    }

    private fun setupTabs() {
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_media)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_links)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_files)))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        showMediaTab()
                    }
                    1 -> {
                        showLinksTab()
                    }
                    2 -> {
                        showFilesTab()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        if (tabs.tabCount > 0) {
            tabs.selectTab(tabs.getTabAt(0))
        }
    }

    private fun setupRecycler() {
        mediaAdapter = SharedMediaAdapter(requireContext())
        linksAdapter = SharedLinksAdapter(requireContext())
        filesAdapter = SharedFilesAdapter(requireContext())

        mediaRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        mediaRecycler.adapter = mediaAdapter
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

                // Update contacts button based on friendship status
                refreshFriendshipStatus()
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

    private fun loadMuteState() {
        val currentUserID = FirebaseAuthentication.currentUserID()
        val otherUserID = userID
        if (currentUserID.isNullOrBlank() || otherUserID.isNullOrBlank()) return

        val chatRoomID = FirebaseChat.getChatRoomID(currentUserID, otherUserID)
        FirebaseChat.getChatRoomReference(chatRoomID)
            .collection("mutes")
            .document(currentUserID)
            .get()
            .addOnSuccessListener { doc ->
                muteUntil = doc.getLong("muteUntil")
                updateMuteUI()
            }
    }

    private fun updateMuteUI() {
        val now = System.currentTimeMillis()
        isMuted = (muteUntil ?: 0L) > now
        if (isMuted) {
            muteIcon.setImageResource(R.drawable.ic_notifications_off)
        } else {
            muteIcon.setImageResource(R.drawable.ic_bell)
        }
    }

    private fun onMuteClicked() {
        val currentUserID = FirebaseAuthentication.currentUserID()
        val otherUserID = userID
        if (currentUserID.isNullOrBlank() || otherUserID.isNullOrBlank()) return

        val chatRoomID = FirebaseChat.getChatRoomID(currentUserID, otherUserID)
        val muteRef = FirebaseChat.getChatRoomReference(chatRoomID)
            .collection("mutes")
            .document(currentUserID)

        val now = System.currentTimeMillis()
        val currentlyMuted = (muteUntil ?: 0L) > now

        if (currentlyMuted) {
            muteRef.delete()
                .addOnSuccessListener {
                    muteUntil = 0L
                    updateMuteUI()
                    Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), it.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            return
        }

        showMuteDialog { selectedUntil ->
            muteRef.set(mapOf("muteUntil" to selectedUntil))
                .addOnSuccessListener {
                    muteUntil = selectedUntil
                    updateMuteUI()
                    Toast.makeText(requireContext(), "Notifications muted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), it.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showMuteDialog(onOk: (Long) -> Unit) {
        val options = arrayOf("5 minutes", "15 minutes", "Until I change")
        var selectedIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle("Mute notifications")
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                val now = System.currentTimeMillis()
                val until = when (selectedIndex) {
                    0 -> now + 5 * 60 * 1000L
                    1 -> now + 15 * 60 * 1000L
                    else -> Long.MAX_VALUE
                }
                onOk(until)
            }
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE)
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.WHITE)
            }
    }

    private fun loadSharedContent() {
        val currentUserID = FirebaseAuthentication.currentUserID()
        val otherUserID = userID
        if (currentUserID.isNullOrBlank() || otherUserID.isNullOrBlank()) {
            return
        }

        val chatRoomID = FirebaseChat.getChatRoomID(currentUserID, otherUserID)
        FirebaseChat.getChatRoomMessagesReference(chatRoomID)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(500)
            .get()
            .addOnSuccessListener { snapshot ->
                sharedMediaItems.clear()
                sharedLinkItems.clear()
                sharedFileItems.clear()

                for (doc in snapshot.documents) {
                    val model = doc.toObject(MsgModel::class.java) ?: continue
                    if (model.isDeleted) continue

                    val messageType = model.messageType
                    val msg = model.msg
                    val imageUrl = model.imageUrl
                    val fileUrl = model.fileUrl
                    val fileName = model.fileName
                    val fileSize = model.fileSize
                    val timestamp = model.timestamp

                    if (SharedContentClassifier.isMediaMessage(messageType, imageUrl, fileUrl, fileName)) {
                        val url = if (!imageUrl.isNullOrBlank()) imageUrl else (fileUrl ?: "")
                        if (url.isNotBlank()) {
                            sharedMediaItems.add(
                                SharedMediaItem(
                                    url = url,
                                    isVideo = SharedContentClassifier.isVideoFile(fileName, fileUrl),
                                    timestamp = timestamp
                                )
                            )
                        }
                    }

                    if (SharedContentClassifier.isLinkMessage(messageType, msg)) {
                        val urls = SharedContentClassifier.extractUrls(msg)
                        urls.forEach { url ->
                            sharedLinkItems.add(SharedLinkItem(url = url, text = msg, timestamp = timestamp))
                        }
                    }

                    if (SharedContentClassifier.isFileMessage(messageType, fileUrl, fileName)) {
                        val url = fileUrl ?: ""
                        if (url.isNotBlank()) {
                            sharedFileItems.add(
                                SharedFileItem(
                                    url = url,
                                    fileName = fileName,
                                    fileSize = fileSize,
                                    timestamp = timestamp
                                )
                            )
                        }
                    }
                }

                sharedLoaded = true
                when (tabs.selectedTabPosition) {
                    1 -> showLinksTab()
                    2 -> showFilesTab()
                    else -> showMediaTab()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserChatSettings", "Failed to load shared content", e)
                sharedLoaded = true
                showEmptyState(getString(R.string.no_shared_media_yet))
            }
    }

    private fun showMediaTab() {
        if (!sharedLoaded) {
            showEmptyState(getString(R.string.loading))
            return
        }

        if (sharedMediaItems.isEmpty()) {
            showEmptyState(getString(R.string.no_shared_media_yet))
            return
        }

        emptyText.visibility = View.GONE
        mediaRecycler.visibility = View.VISIBLE
        mediaRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        mediaRecycler.adapter = mediaAdapter
        mediaAdapter.submit(sharedMediaItems)
    }

    private fun showLinksTab() {
        if (!sharedLoaded) {
            showEmptyState(getString(R.string.loading))
            return
        }

        if (sharedLinkItems.isEmpty()) {
            showEmptyState(getString(R.string.no_shared_links_yet))
            return
        }

        emptyText.visibility = View.GONE
        mediaRecycler.visibility = View.VISIBLE
        mediaRecycler.layoutManager = LinearLayoutManager(requireContext())
        mediaRecycler.adapter = linksAdapter
        linksAdapter.submit(sharedLinkItems)
    }

    private fun showFilesTab() {
        if (!sharedLoaded) {
            showEmptyState(getString(R.string.loading))
            return
        }

        if (sharedFileItems.isEmpty()) {
            showEmptyState(getString(R.string.no_shared_files_yet))
            return
        }

        emptyText.visibility = View.GONE
        mediaRecycler.visibility = View.VISIBLE
        mediaRecycler.layoutManager = LinearLayoutManager(requireContext())
        mediaRecycler.adapter = filesAdapter
        filesAdapter.submit(sharedFileItems)
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
        val targetUserID = userID
        if (targetUserID.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.user_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        val targetName = user?.username ?: getString(R.string.this_user)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.block_user_title))
            .setMessage(getString(R.string.block_user_message, targetName))
            .setPositiveButton(getString(R.string.block_action)) { _, _ ->
                FirebaseBlocking.blockUser(
                    targetUserID,
                    onSuccess = {
                        Toast.makeText(requireContext(), getString(R.string.blocked_user_success, targetName), Toast.LENGTH_SHORT).show()
                        activity?.finish()
                    },
                    onFailure = { e ->
                        Toast.makeText(requireContext(), getString(R.string.failed_to_block, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showReportUserDialog() {
        // TODO: Show report dialog
        Toast.makeText(requireContext(), getString(R.string.report_user_title), Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteChatDialog() {
        val currentUserID = FirebaseAuthentication.currentUserID()
        val otherUserID = userID

        if (currentUserID.isNullOrBlank() || otherUserID.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.chat_not_available), Toast.LENGTH_SHORT).show()
            return
        }

        val chatRoomID = FirebaseChat.getChatRoomID(currentUserID, otherUserID)
        val targetName = user?.username ?: getString(R.string.this_user)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_chat_title))
            .setMessage(getString(R.string.delete_chat_message))
            .setPositiveButton(getString(R.string.delete_action)) { _, _ ->
                FirebaseChat.softDeleteChatRoom(
                    chatRoomID,
                    onSuccess = {
                        Toast.makeText(requireContext(), getString(R.string.chat_deleted), Toast.LENGTH_SHORT).show()
                        activity?.finish()
                    },
                    onFailure = { e ->
                        Toast.makeText(requireContext(), getString(R.string.failed_to_delete_chat, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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
