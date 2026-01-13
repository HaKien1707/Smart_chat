package com.example.smart_chat.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.activities.user_chat.ChatActivity
import com.example.smart_chat.R
import com.example.smart_chat.adapters.community.CommunityMemberAdapter
import com.example.smart_chat.adapters.shared.SharedFilesAdapter
import com.example.smart_chat.adapters.shared.SharedLinksAdapter
import com.example.smart_chat.adapters.shared.SharedMediaAdapter
import com.example.smart_chat.models.community.CommunityMsgModel
import com.example.smart_chat.models.community.CommunityModel
import com.example.smart_chat.models.shared.SharedFileItem
import com.example.smart_chat.models.shared.SharedLinkItem
import com.example.smart_chat.models.shared.SharedMediaItem
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseCommunity
import com.example.smart_chat.utils.firebase.FirebaseNotifications
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.shared.SharedContentClassifier
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream

class CommunitySettingsFragment : Fragment() {

    private lateinit var backBtn: ImageButton
    private lateinit var editBtn: ImageButton
    private lateinit var moreBtn: ImageButton
    private lateinit var communityImage: ImageView
    private lateinit var communityName: TextView
    private lateinit var membersCount: TextView
    private lateinit var messageBtn: LinearLayout
    private lateinit var muteBtn: LinearLayout
    private lateinit var muteIcon: ImageView
    private lateinit var leaveBtn: LinearLayout
    private lateinit var descriptionValue: TextView
    private lateinit var inviteLinkValue: TextView
    private lateinit var tabs: TabLayout
    private lateinit var membersRecycler: RecyclerView
    private lateinit var sharedRecycler: RecyclerView
    private lateinit var emptyText: TextView

    private var communityID: String? = null
    private var community: CommunityModel? = null
    private val membersList = mutableListOf<userModel>()
    private var adapter: CommunityMemberAdapter? = null

    private var currentUserIsOwner: Boolean = false
    private var currentUserIsAdmin: Boolean = false
    private var muteUntil: Long? = null

    private lateinit var mediaAdapter: SharedMediaAdapter
    private lateinit var linksAdapter: SharedLinksAdapter
    private lateinit var filesAdapter: SharedFilesAdapter
    private var sharedLoaded: Boolean = false
    private val sharedMediaItems = mutableListOf<SharedMediaItem>()
    private val sharedLinkItems = mutableListOf<SharedLinkItem>()
    private val sharedFileItems = mutableListOf<SharedFileItem>()

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    updateCommunityImage(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            communityID = it.getString("communityID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (communityID == null) {
            activity?.finish()
            return
        }

        initViews(view)
        setupListeners()
        setupTabs()
        setupRecycler()
        loadCommunityDetails()
        loadMembers()
        loadSharedContent()
        loadMuteState()
    }

    private fun initViews(view: View) {
        backBtn = view.findViewById(R.id.back_btn)
        editBtn = view.findViewById(R.id.edit_btn)
        moreBtn = view.findViewById(R.id.more_btn)
        communityImage = view.findViewById(R.id.community_image)
        communityName = view.findViewById(R.id.community_name)
        membersCount = view.findViewById(R.id.members_count)
        messageBtn = view.findViewById(R.id.message_btn)
        muteBtn = view.findViewById(R.id.mute_btn)
        muteIcon = view.findViewById(R.id.mute_icon)
        leaveBtn = view.findViewById(R.id.leave_btn)
        descriptionValue = view.findViewById(R.id.description_value)
        inviteLinkValue = view.findViewById(R.id.invite_link_value)
        tabs = view.findViewById(R.id.tabs)
        membersRecycler = view.findViewById(R.id.members_recycler)
        sharedRecycler = view.findViewById(R.id.shared_recycler)
        emptyText = view.findViewById(R.id.empty_text)
    }

    private fun updateMuteUI() {
        val now = System.currentTimeMillis()
        val isMutedNow = (muteUntil ?: 0L) > now
        if (isMutedNow) {
            muteIcon.setImageResource(R.drawable.ic_notifications_off)
        } else {
            muteIcon.setImageResource(R.drawable.ic_bell)
        }
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            activity?.finish()
        }

        communityImage.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(512)
                .maxResultSize(512, 512)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }

        editBtn.setOnClickListener {
            if (!currentUserIsOwner && !currentUserIsAdmin) return@setOnClickListener
            showEditCommunityNameDialog()
        }

        moreBtn.setOnClickListener {
            if (!currentUserIsOwner && !currentUserIsAdmin) return@setOnClickListener
            showOwnerMoreOptionsMenu()
        }

        messageBtn.setOnClickListener {
            // Go back to community chat
            activity?.finish()
        }

        muteBtn.setOnClickListener {
            onMuteClicked()
        }

        leaveBtn.setOnClickListener {
            handleLeaveCommunity()
        }
    }

    private fun handleLeaveCommunity() {
        if (currentUserIsOwner) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.cannot_leave))
                .setMessage(getString(R.string.cannot_leave_community_owner_message))
                .setPositiveButton(getString(R.string.ok), null)
                .show()
                .apply {
                    applyAccentToDialogButtons(this)
                }
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.leave_community_title))
            .setMessage(getString(R.string.leave_community_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val id = communityID ?: return@setPositiveButton
                val currentUserId = FirebaseAuthentication.currentUserID() ?: return@setPositiveButton

                val updates = mutableMapOf<String, Any>(
                    "bannedUserIDs" to FieldValue.arrayUnion(currentUserId)
                )

                // If an admin leaves, remove their admin role too.
                if (currentUserIsAdmin) {
                    updates["adminIDs"] = FieldValue.arrayRemove(currentUserId)
                }

                FirebaseCommunity.getCommunityReference(id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), getString(R.string.left_community), Toast.LENGTH_SHORT).show()
                        activity?.finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
            .apply {
                applyAccentToDialogButtons(this)
            }
    }

    private fun applyAccentToDialogButtons(dialog: AlertDialog) {
        val accent = ContextCompat.getColor(requireContext(), R.color.settings_accent)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(accent)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accent)
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(accent)
    }

    private fun setupTabs() {
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_members)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_media)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_links)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_files)))

        membersRecycler.visibility = View.VISIBLE
        sharedRecycler.visibility = View.GONE
        emptyText.visibility = View.GONE

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Show members
                        membersRecycler.visibility = View.VISIBLE
                        sharedRecycler.visibility = View.GONE
                        emptyText.visibility = View.GONE
                    }
                    1 -> {
                        // Show media
                        membersRecycler.visibility = View.GONE
                        showMediaTab()
                    }
                    2 -> {
                        // Show links
                        membersRecycler.visibility = View.GONE
                        showLinksTab()
                    }
                    3 -> {
                        // Show files
                        membersRecycler.visibility = View.GONE
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

        val ownerId = community?.ownerID ?: community?.adminID
        val adminIds = community?.adminIDs?.filterNotNull()?.toSet().orEmpty()
        adapter = CommunityMemberAdapter(
            context = requireContext(),
            membersList = membersList,
            ownerID = ownerId,
            adminIDs = adminIds,
            currentUserID = FirebaseAuthentication.currentUserID(),
            onChatMember = { user ->
                openChatWithMember(user)
            },
            onAddAdmin = { userId ->
                addAdminForMember(userId)
            },
            onRemoveAdmin = { userId ->
                removeAdminForMember(userId)
            },
            onRemoveMember = { user ->
                removeMemberFromCommunity(user)
            }
        )
        membersRecycler.layoutManager = LinearLayoutManager(requireContext())
        membersRecycler.adapter = adapter
    }

    private fun removeMemberFromCommunity(user: userModel) {
        if (!currentUserIsOwner && !currentUserIsAdmin) return

        val id = communityID ?: return
        val memberId = user.userID ?: return
        val ownerId = community?.ownerID ?: community?.adminID
        val adminIds = community?.adminIDs?.filterNotNull()?.toSet().orEmpty()

        val isOwnerMember = !ownerId.isNullOrBlank() && memberId == ownerId
        val isAdminMember = adminIds.contains(memberId)

        if (isOwnerMember) return
        if (memberId == FirebaseAuthentication.currentUserID()) return

        // Admin cannot remove admins/owner.
        if (currentUserIsAdmin && !currentUserIsOwner && isAdminMember) return

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.remove_member_title))
            .setMessage(getString(R.string.remove_member_message, user.username ?: getString(R.string.this_user)))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.remove_action)) { _, _ ->
                val updates = mutableMapOf<String, Any>(
                    "bannedUserIDs" to FieldValue.arrayUnion(memberId)
                )

                // If owner removes an admin, also remove from adminIDs.
                if (currentUserIsOwner && isAdminMember) {
                    updates["adminIDs"] = FieldValue.arrayRemove(memberId)
                }

                FirebaseCommunity.getCommunityReference(id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), getString(R.string.member_removed), Toast.LENGTH_SHORT).show()

                        FirebaseNotifications.createNotification(
                            type = "BANNED_FROM_COMMUNITY",
                            recipientID = memberId,
                            senderID = FirebaseAuthentication.currentUserID() ?: "",
                            senderName = if (currentUserIsOwner) "Owner" else "Admin",
                            communityID = id,
                            communityName = community?.communityName,
                            message = "You have been removed from ${community?.communityName}"
                        )

                        loadCommunityDetails()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
            .apply {
                applyAccentToDialogButtons(this)
            }
    }

    private fun addAdminForMember(userId: String) {
        if (!currentUserIsOwner) return
        val id = communityID ?: return
        val ownerId = community?.ownerID ?: community?.adminID
        if (userId == ownerId) return

        FirebaseCommunity.getCommunityReference(id)
            .update("adminIDs", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.admin_added), Toast.LENGTH_SHORT).show()
                loadCommunityDetails()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeAdminForMember(userId: String) {
        if (!currentUserIsOwner) return
        val id = communityID ?: return
        val ownerId = community?.ownerID ?: community?.adminID
        if (userId == ownerId) return

        FirebaseCommunity.getCommunityReference(id)
            .update("adminIDs", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.admin_removed), Toast.LENGTH_SHORT).show()
                loadCommunityDetails()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSharedContent() {
        val id = communityID ?: return
        FirebaseCommunity.getCommunityMessagesReference(id)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(500)
            .get()
            .addOnSuccessListener { snapshot ->
                sharedMediaItems.clear()
                sharedLinkItems.clear()
                sharedFileItems.clear()

                for (doc in snapshot.documents) {
                    val model = doc.toObject(CommunityMsgModel::class.java) ?: continue
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
                    1 -> showMediaTab()
                    2 -> showLinksTab()
                    3 -> showFilesTab()
                }
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to load shared content", e)
                sharedLoaded = true
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
        sharedRecycler.visibility = View.VISIBLE
        sharedRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        sharedRecycler.adapter = mediaAdapter
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
        sharedRecycler.visibility = View.VISIBLE
        sharedRecycler.layoutManager = LinearLayoutManager(requireContext())
        sharedRecycler.adapter = linksAdapter
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
        sharedRecycler.visibility = View.VISIBLE
        sharedRecycler.layoutManager = LinearLayoutManager(requireContext())
        sharedRecycler.adapter = filesAdapter
        filesAdapter.submit(sharedFileItems)
    }

    private fun showEmptyState(message: String) {
        sharedRecycler.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyText.text = message
    }

    private fun loadCommunityDetails() {
        FirebaseCommunity.getCommunityReference(communityID!!).get()
            .addOnSuccessListener { document ->
                community = document.toObject(CommunityModel::class.java)

                communityName.text = community?.communityName ?: "Community"

                descriptionValue.text = community?.communityDescription ?: ""
                inviteLinkValue.text = "smartchat://community/${communityID ?: ""}"

                // Load community image
                val imageUrl = community?.communityImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(requireContext(), imageUrl, communityImage)
                } else {
                    communityImage.setImageResource(R.drawable.ic_community)
                }

                val currentUserId = FirebaseAuthentication.currentUserID()
                val ownerId = community?.ownerID ?: community?.adminID
                val adminIds = community?.adminIDs?.filterNotNull()?.toSet().orEmpty()
                currentUserIsOwner = !ownerId.isNullOrBlank() && ownerId == currentUserId
                currentUserIsAdmin = adminIds.contains(currentUserId)
                moreBtn.visibility = if (currentUserIsOwner || currentUserIsAdmin) View.VISIBLE else View.GONE
                editBtn.visibility = if (currentUserIsOwner || currentUserIsAdmin) View.VISIBLE else View.GONE

                // Enforce exactly one owner and keep adminIDs excluding owner (backfill legacy docs).
                if (!ownerId.isNullOrBlank()) {
                    val currentAdminIds = community?.adminIDs?.filterNotNull()?.toSet().orEmpty()
                    val cleanedAdmins = currentAdminIds.filter { it != ownerId }.toList()

                    val needsOwnerBackfill = community?.ownerID.isNullOrBlank()
                    val ownerInAdmins = currentAdminIds.contains(ownerId)
                    val needsAdminsInit = community?.adminIDs == null

                    if (needsOwnerBackfill || ownerInAdmins || needsAdminsInit) {
                        community?.ownerID = ownerId
                        community?.adminIDs = cleanedAdmins.toMutableList()
                        FirebaseCommunity.getCommunityReference(communityID!!)
                            .update(mapOf("ownerID" to ownerId, "adminIDs" to cleanedAdmins))
                    }
                }

                adapter?.updateRoles(ownerId, adminIds)

                // Refresh ordering/labels immediately after role changes.
                loadMembers()
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to load community", e)
                Toast.makeText(requireContext(), "Failed to load community", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChatWithMember(user: userModel) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
        androidUtils.passUserModelAsIntent(intent, user)
        startActivity(intent)
    }

    private fun showOwnerMoreOptionsMenu() {
        val popupMenu = android.widget.PopupMenu(requireContext(), moreBtn)
        popupMenu.menuInflater.inflate(R.menu.menu_community_settings_owner, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_community_type -> {
                    showCommunityTypeDialog()
                    true
                }
                R.id.action_add_admins -> {
                    showAddAdminsDialog()
                    true
                }
                R.id.action_delete_community -> {
                    showDeleteCommunityDialog()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showCommunityTypeDialog() {
        val id = communityID ?: return
        val currentType = (community?.communityType ?: "public").lowercase()
        val options = arrayOf(
            getString(R.string.community_public),
            getString(R.string.community_private)
        )
        var selectedIndex = if (currentType == "private") 1 else 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.community_type_title))
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val newType = if (selectedIndex == 1) "private" else "public"
                FirebaseCommunity.getCommunityReference(id)
                    .update("communityType", newType)
                    .addOnSuccessListener {
                        community?.communityType = newType
                        Toast.makeText(requireContext(), getString(R.string.updated), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
            .apply {
                applyAccentToDialogButtons(this)
            }
    }

    private fun showAddAdminsDialog() {
        val id = communityID ?: return
        val ownerId = community?.ownerID ?: community?.adminID
        val existingAdmins = community?.adminIDs?.toSet().orEmpty()

        val candidates = membersList
            .mapNotNull { user ->
                val uid = user.userID
                if (uid.isNullOrBlank()) return@mapNotNull null
                if (uid == ownerId) return@mapNotNull null
                if (existingAdmins.contains(uid)) return@mapNotNull null
                uid to (user.username ?: "Unknown")
            }
            .distinctBy { it.first }

        if (candidates.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_users_to_promote), Toast.LENGTH_SHORT).show()
            return
        }

        val names = candidates.map { it.second }.toTypedArray()
        val checked = BooleanArray(candidates.size)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_admin_title))
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val selectedIds = candidates
                    .filterIndexed { index, _ -> checked[index] }
                    .map { it.first }

                if (selectedIds.isEmpty()) return@setPositiveButton

                FirebaseCommunity.getCommunityReference(id)
                    .update("adminIDs", FieldValue.arrayUnion(*selectedIds.toTypedArray()))
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), getString(R.string.admins_updated), Toast.LENGTH_SHORT).show()
                        // Refresh cached community object
                        loadCommunityDetails()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
            .apply {
                applyAccentToDialogButtons(this)
            }
    }

    private fun showDeleteCommunityDialog() {
        val id = communityID ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_community_title))
            .setMessage(getString(R.string.delete_community_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete_action)) { _, _ ->
                FirebaseCommunity.getCommunityReference(id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), getString(R.string.community_deleted), Toast.LENGTH_SHORT).show()
                        activity?.finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
            .apply {
                applyAccentToDialogButtons(this)
            }
    }

    private fun loadMuteState() {
        val id = communityID ?: return
        val currentUserId = FirebaseAuthentication.currentUserID() ?: return

        FirebaseCommunity.getCommunityReference(id)
            .collection("mutes")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                muteUntil = doc.getLong("muteUntil")
                updateMuteUI()
            }
            .addOnFailureListener { e ->
                Log.e(
                    "CommunitySettingsMute",
                    "Failed to load mute state for communities/$id/mutes/$currentUserId",
                    e
                )
                updateMuteUI()
            }
    }

    private fun onMuteClicked() {
        val id = communityID ?: return
        val currentUserId = FirebaseAuthentication.currentUserID() ?: return

        val now = System.currentTimeMillis()
        val isCurrentlyMuted = (muteUntil ?: 0L) > now

        val muteRef = FirebaseCommunity.getCommunityReference(id)
            .collection("mutes")
            .document(currentUserId)

        if (isCurrentlyMuted) {
            muteRef.delete()
                .addOnSuccessListener {
                    muteUntil = 0L
                    updateMuteUI()
                    Toast.makeText(requireContext(), getString(R.string.notifications_enabled), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.e(
                        "CommunitySettingsMute",
                        "Failed to unmute at communities/$id/mutes/$currentUserId",
                        it
                    )
                    Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
                }
            return
        }

        showMuteDialog { selectedUntil ->
            muteRef.set(mapOf("muteUntil" to selectedUntil))
                .addOnSuccessListener {
                    muteUntil = selectedUntil
                    updateMuteUI()
                    Toast.makeText(requireContext(), getString(R.string.notifications_muted), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.e(
                        "CommunitySettingsMute",
                        "Failed to mute at communities/$id/mutes/$currentUserId",
                        it
                    )
                    Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showMuteDialog(onOk: (Long) -> Unit) {
        val options = arrayOf(
            getString(R.string.mute_5_minutes),
            getString(R.string.mute_15_minutes),
            getString(R.string.mute_until_i_change)
        )
        var selectedIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.mute_notifications_title))
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
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
                applyAccentToDialogButtons(this)
            }
    }

    private fun loadMembers() {
        // Load all users from Firestore
        FirebaseAuthentication.allUsersCollection().get()
            .addOnSuccessListener { documents ->
                membersList.clear()
                var totalMembers = 0

                val bannedUserIDs = community?.bannedUserIDs ?: emptyList()

                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    if (user != null) {
                        if (user.userID.isNullOrBlank()) {
                            user.userID = doc.id
                        }
                        if (!user.userID.isNullOrBlank() && bannedUserIDs.contains(user.userID)) {
                            continue
                        }
                        membersList.add(user)
                        totalMembers++
                    }
                }

                val ownerId = community?.ownerID ?: community?.adminID
                val adminIds = community?.adminIDs?.filterNotNull()?.toSet().orEmpty()

                // Sort: Owner first, then Admins, then others
                membersList.sortWith(
                    compareBy<userModel> {
                        when {
                            it.userID == ownerId -> 0
                            it.userID != null && adminIds.contains(it.userID!!) -> 1
                            else -> 2
                        }
                    }.thenBy { it.username ?: "" }
                )

                membersCount.text = resources.getQuantityString(R.plurals.memberCount, totalMembers, totalMembers)
                adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to load members", e)
                Toast.makeText(requireContext(), getString(R.string.failed_to_load_members), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditCommunityNameDialog() {
        val editText = EditText(requireContext()).apply {
            setText(communityName.text)
            setSingleLine()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_community_name))
            .setView(editText)
            .setPositiveButton(getString(R.string.saveBTN)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != community?.communityName) {
                    updateCommunityName(newName)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show().apply {
                applyAccentToDialogButtons(this)
            }
    }

    private fun updateCommunityName(newName: String) {
        FirebaseCommunity.getCommunityReference(communityID!!).update("communityName", newName)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.community_name_updated), Toast.LENGTH_SHORT).show()
                communityName.text = newName
                community?.communityName = newName
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to update name", e)
                Toast.makeText(requireContext(), getString(R.string.failed_to_update_name), Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCommunityImage(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            FirebaseCommunity.getCommunityReference(communityID!!).update("communityImage", base64)
                .addOnSuccessListener {
                    androidUtils.setProfileImageFromBase64(requireContext(), base64, communityImage)
                    Toast.makeText(requireContext(), "Community photo updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update photo", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("CommunitySettings", "Failed to update image", e)
            Toast.makeText(requireContext(), "Failed to update photo", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(communityID: String): CommunitySettingsFragment {
            return CommunitySettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("communityID", communityID)
                }
            }
        }
    }
}
