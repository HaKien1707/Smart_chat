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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.activities.group_chat.AddMembersActivity
import com.example.smart_chat.activities.user_chat.ChatActivity
import com.example.smart_chat.adapters.group.GroupMemberAdapter
import com.example.smart_chat.adapters.shared.SharedFilesAdapter
import com.example.smart_chat.adapters.shared.SharedLinksAdapter
import com.example.smart_chat.adapters.shared.SharedMediaAdapter
import com.example.smart_chat.models.group.GroupMsgModel
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.models.shared.SharedFileItem
import com.example.smart_chat.models.shared.SharedLinkItem
import com.example.smart_chat.models.shared.SharedMediaItem
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseGroups
import com.example.smart_chat.utils.firebase.FirebaseNotifications
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.shared.SharedContentClassifier
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class GroupSettingsFragment : Fragment() {

    private lateinit var backBtn: ImageButton
    private lateinit var editBtn: ImageButton
    private lateinit var moreBtn: ImageButton
    private lateinit var groupImage: ImageView
    private lateinit var groupName: TextView
    private lateinit var membersCount: TextView
    private lateinit var messageBtn: LinearLayout
    private lateinit var muteBtn: LinearLayout
    private lateinit var muteIcon: ImageView
    private lateinit var leaveBtn: LinearLayout
    private lateinit var addMembersBtn: LinearLayout
    private lateinit var tabs: TabLayout
    private lateinit var membersRecycler: RecyclerView
    private lateinit var sharedRecycler: RecyclerView
    private lateinit var emptyText: TextView

    private var groupID: String? = null
    private var group: groupModel? = null
    private val membersList = mutableListOf<Pair<userModel, Boolean>>()
    private var adapter: GroupMemberAdapter? = null
    private var currentUserIsAdmin = false
    private var currentUserIsOwner = false

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
                    updateGroupImage(uri)
                }
            }
        }

    private val addMembersLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                loadGroupDetails()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupID = it.getString("groupID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_group_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (groupID == null) {
            activity?.finish()
            return
        }

        initViews(view)
        setupListeners()
        setupTabs()
        setupRecycler()
        loadGroupDetails()
        loadSharedContent()
        loadMuteState()
    }

    private fun initViews(view: View) {
        backBtn = view.findViewById(R.id.back_btn)
        editBtn = view.findViewById(R.id.edit_btn)
        moreBtn = view.findViewById(R.id.more_btn)
        groupImage = view.findViewById(R.id.group_image)
        groupName = view.findViewById(R.id.group_name)
        membersCount = view.findViewById(R.id.members_count)
        messageBtn = view.findViewById(R.id.message_btn)
        muteBtn = view.findViewById(R.id.mute_btn)
        muteIcon = view.findViewById(R.id.mute_icon)
        leaveBtn = view.findViewById(R.id.leave_btn)
        addMembersBtn = view.findViewById(R.id.add_members_btn)
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

        groupImage.setOnClickListener {
            if (currentUserIsAdmin) {
                ImagePicker.with(this)
                    .cropSquare()
                    .compress(512)
                    .maxResultSize(512, 512)
                    .createIntent { intent -> imagePickerLauncher.launch(intent) }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.only_admins_can_change_group_photo),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        editBtn.setOnClickListener {
            showEditGroupNameDialog()
        }

        moreBtn.setOnClickListener {
            if (!currentUserIsAdmin) return@setOnClickListener
            showOwnerMoreOptionsMenu()
        }

        messageBtn.setOnClickListener {
            // Go back to group chat
            activity?.finish()
        }

        muteBtn.setOnClickListener {
            onMuteClicked()
        }

        leaveBtn.setOnClickListener {
            confirmLeaveGroup()
        }

        addMembersBtn.setOnClickListener {
            if (!currentUserIsAdmin) {
                Toast.makeText(requireContext(), getString(R.string.only_admins_can_add_members), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = groupID ?: return@setOnClickListener
            val intent = Intent(requireContext(), AddMembersActivity::class.java)
            intent.putExtra("groupID", id)
            addMembersLauncher.launch(intent)
        }
    }

    private fun setupTabs() {
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_members)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_media)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_links)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_files)))

        // Set default visibility for members tab
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

        // Select first tab (Members) by default
        if (tabs.tabCount > 0) {
            tabs.selectTab(tabs.getTabAt(0))
        }
    }

    private fun setupRecycler() {
        mediaAdapter = SharedMediaAdapter(requireContext())
        linksAdapter = SharedLinksAdapter(requireContext())
        filesAdapter = SharedFilesAdapter(requireContext())

        adapter = GroupMemberAdapter(
            members = membersList,
            context = requireContext(),
            currentUserIsAdmin = currentUserIsAdmin,
            currentUserIsOwner = currentUserIsOwner,
            currentUserID = FirebaseAuthentication.currentUserID(),
            ownerID = group?.ownerID,
            onChatMember = { user ->
                openChatWithMember(user)
            },
            onAddAdmin = { userId ->
                addAdminForMember(userId)
            },
            onRemoveAdmin = { userId ->
                removeAdminForMember(userId)
            },
            onRemoveMember = { userID ->
                removeMember(userID)
            }
        )
        membersRecycler.layoutManager = LinearLayoutManager(requireContext())
        membersRecycler.adapter = adapter
    }

    private fun addAdminForMember(userId: String) {
        if (!currentUserIsOwner) return
        val id = groupID ?: return

        // Owner is not an admin
        if (userId == group?.ownerID) return

        FirebaseGroups.getGroupReference(id)
            .update("adminIDs", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.admin_added), Toast.LENGTH_SHORT).show()
                loadGroupDetails()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeAdminForMember(userId: String) {
        if (!currentUserIsOwner) return
        val id = groupID ?: return

        // Never remove owner from admin list
        if (userId == group?.ownerID) return

        FirebaseGroups.getGroupReference(id)
            .update("adminIDs", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.admin_removed), Toast.LENGTH_SHORT).show()
                loadGroupDetails()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadGroupDetails() {
        FirebaseGroups.getGroupReference(groupID!!).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                groupName.text = group?.groupName ?: "Group"

                // Load group image
                val imageUrl = group?.groupImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(requireContext(), imageUrl, groupImage)
                } else {
                    groupImage.setImageResource(R.drawable.ic_group)
                }

                val currentUserId = FirebaseAuthentication.currentUserID()

                // Enforce 1 owner (backfill from legacy schema if needed)
                val legacyOwner = group?.adminIDs?.filterNotNull()?.firstOrNull()
                val computedOwner = group?.ownerID ?: legacyOwner ?: group?.createdBy
                if (group?.ownerID.isNullOrBlank() && !computedOwner.isNullOrBlank()) {
                    group?.ownerID = computedOwner

                    // Remove owner from adminIDs to keep 0..n admins semantics
                    val admins = group?.adminIDs?.filterNotNull()?.filter { it != computedOwner }?.distinct() ?: emptyList()
                    group?.adminIDs = admins.toMutableList()

                    FirebaseGroups.getGroupReference(groupID!!)
                        .update(mapOf("ownerID" to computedOwner, "adminIDs" to admins))
                } else if (!computedOwner.isNullOrBlank()) {
                    // Also ensure owner is not duplicated inside adminIDs
                    val admins = group?.adminIDs?.filterNotNull()?.filter { it != computedOwner }?.distinct() ?: emptyList()
                    if (admins.size != (group?.adminIDs?.filterNotNull()?.size ?: 0)) {
                        group?.adminIDs = admins.toMutableList()
                        FirebaseGroups.getGroupReference(groupID!!)
                            .update("adminIDs", admins)
                    }
                }

                val ownerId = group?.ownerID
                val isAdmin = group?.adminIDs?.contains(currentUserId) == true
                currentUserIsOwner = !ownerId.isNullOrBlank() && ownerId == currentUserId

                // Staff permissions (owner OR admin)
                currentUserIsAdmin = currentUserIsOwner || isAdmin

                moreBtn.visibility = if (currentUserIsAdmin) View.VISIBLE else View.GONE

                addMembersBtn.visibility = if (currentUserIsAdmin) View.VISIBLE else View.GONE

                // Refresh UI that depends on admin state + group members
                setupRecycler()
                loadMembers()
            }
            .addOnFailureListener { e ->
                Log.e("GroupSettings", "Failed to load group", e)
                Toast.makeText(requireContext(), getString(R.string.failed_to_load_group), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showOwnerMoreOptionsMenu() {
        val popupMenu = android.widget.PopupMenu(requireContext(), moreBtn)
        popupMenu.menuInflater.inflate(R.menu.menu_group_settings_owner, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_admin -> {
                    showAddAdminDialog()
                    true
                }
                R.id.action_delete_group -> {
                    showDeleteGroupDialog()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showAddAdminDialog() {
        val candidateUsers = membersList
            .mapNotNull { (user, isAdmin) ->
                val id = user.userID
                if (id.isNullOrBlank()) return@mapNotNull null
                if (isAdmin) return@mapNotNull null
                // Owner can promote any non-admin member
                id to (user.username ?: "Unknown")
            }
            .distinctBy { it.first }

        if (candidateUsers.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_members_to_promote), Toast.LENGTH_SHORT).show()
            return
        }

        val names = candidateUsers.map { it.second }.toTypedArray()
        val checked = BooleanArray(candidateUsers.size)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_admin_title))
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val selectedIds = candidateUsers
                    .filterIndexed { index, _ -> checked[index] }
                    .map { it.first }

                if (selectedIds.isEmpty()) return@setPositiveButton

                val id = groupID ?: return@setPositiveButton
                FirebaseGroups.getGroupReference(id)
                    .update("adminIDs", FieldValue.arrayUnion(*selectedIds.toTypedArray()))
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), getString(R.string.admins_updated), Toast.LENGTH_SHORT).show()
                        loadGroupDetails()
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

    private fun showDeleteGroupDialog() {
        val id = groupID ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_group_title))
            .setMessage(getString(R.string.delete_group_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete_action)) { _, _ ->
                FirebaseGroups.getGroupReference(id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), getString(R.string.group_deleted), Toast.LENGTH_SHORT).show()
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
        val id = groupID ?: return
        val currentUserId = FirebaseAuthentication.currentUserID() ?: return

        FirebaseGroups.getGroupReference(id)
            .collection("mutes")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                val until = doc.getLong("muteUntil")
                muteUntil = until
                updateMuteUI()
            }
            .addOnFailureListener { e ->
                Log.e("GroupSettingsMute", "Failed to load mute state for chatgroups/$id/mutes/$currentUserId", e)
                updateMuteUI()
            }
    }

    private fun onMuteClicked() {
        val id = groupID ?: return
        val currentUserId = FirebaseAuthentication.currentUserID() ?: return

        val now = System.currentTimeMillis()
        val isCurrentlyMuted = (muteUntil ?: 0L) > now

        if (isCurrentlyMuted) {
            FirebaseGroups.getGroupReference(id)
                .collection("mutes")
                .document(currentUserId)
                .delete()
                .addOnSuccessListener {
                    muteUntil = 0L
                    updateMuteUI()
                    Toast.makeText(requireContext(), getString(R.string.notifications_enabled), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.e(
                        "GroupSettingsMute",
                        "Failed to unmute at chatgroups/$id/mutes/$currentUserId",
                        it
                    )
                    Toast.makeText(requireContext(), it.message ?: getString(R.string.failed), Toast.LENGTH_SHORT).show()
                }
            return
        }

        showMuteDialog { selectedUntil ->
            FirebaseGroups.getGroupReference(id)
                .collection("mutes")
                .document(currentUserId)
                .set(mapOf("muteUntil" to selectedUntil))
                .addOnSuccessListener {
                    muteUntil = selectedUntil
                    updateMuteUI()
                    Toast.makeText(requireContext(), getString(R.string.notifications_muted), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.e(
                        "GroupSettingsMute",
                        "Failed to mute at chatgroups/$id/mutes/$currentUserId",
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

    private fun confirmLeaveGroup() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.leaveGroup))
            .setMessage(getString(R.string.leave_group_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                leaveGroup()
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

    private fun leaveGroup() {
        val id = groupID ?: return
        val currentUserId = FirebaseAuthentication.currentUserID() ?: return
        val groupRef = FirebaseGroups.getGroupReference(id)

        FirebaseFirestore.getInstance().runTransaction { tx ->
            val snap = tx.get(groupRef)
            val g = snap.toObject(groupModel::class.java) ?: return@runTransaction null

            val members = (g.memberIDs ?: mutableListOf()).filterNotNull().toMutableList()
            val admins = (g.adminIDs ?: mutableListOf()).filterNotNull().toMutableList()
            var ownerId = g.ownerID ?: g.createdBy ?: admins.firstOrNull()

            if (!members.contains(currentUserId)) return@runTransaction null

            members.remove(currentUserId)
            admins.remove(currentUserId)

            // Ensure owner is not duplicated in admins list
            if (!ownerId.isNullOrBlank()) {
                admins.removeAll { it == ownerId }
            }

            if (members.isEmpty()) {
                tx.delete(groupRef)
                return@runTransaction null
            }

            // If the leaving user is the owner, reassign owner to a random remaining member.
            if (!ownerId.isNullOrBlank() && ownerId == currentUserId) {
                ownerId = members[Random.nextInt(members.size)]
                // Owner should not appear in admins.
                admins.removeAll { it == ownerId }
            }

            // Enforce exactly one owner
            if (ownerId.isNullOrBlank()) {
                ownerId = members[Random.nextInt(members.size)]
                admins.removeAll { it == ownerId }
            }

            tx.update(
                groupRef,
                mapOf(
                    "memberIDs" to members,
                    "adminIDs" to admins,
                    "ownerID" to ownerId
                )
            )

            null
        }
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.left_group), Toast.LENGTH_SHORT)
                    .show()
                activity?.finish()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    it.message ?: getString(R.string.failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadMembers() {
        val memberIds = group?.memberIDs?.filterNotNull()?.toSet().orEmpty()

        if (memberIds.isEmpty()) {
            membersList.clear()
            membersCount.text = resources.getQuantityString(R.plurals.memberCount, 0, 0)
            adapter?.notifyDataSetChanged()
            return
        }

        // Load all users from Firestore, then filter to group members
        FirebaseAuthentication.allUsersCollection().get()
            .addOnSuccessListener { documents ->
                membersList.clear()
                var totalMembers = 0

                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    if (user.userID.isNullOrBlank()) {
                        user.userID = doc.id
                    }
                    val userId = user.userID
                    if (userId != null && memberIds.contains(userId)) {
                        val isAdmin = userId != group?.ownerID && group?.adminIDs?.contains(userId) == true
                        membersList.add(Pair(user, isAdmin))
                        totalMembers++
                    }
                }

                val ownerId = group?.ownerID
                membersList.sortWith(
                    compareBy<Pair<userModel, Boolean>> {
                        val userId = it.first.userID
                        val isAdmin = it.second
                        when {
                            userId == ownerId -> 0
                            isAdmin -> 1
                            else -> 2
                        }
                    }.thenBy { it.first.username ?: "" }
                )

                membersCount.text = resources.getQuantityString(R.plurals.memberCount, totalMembers, totalMembers)
                adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("GroupSettings", "Failed to load members", e)
                Toast.makeText(requireContext(), getString(R.string.failed_to_load_members), Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun openChatWithMember(user: userModel) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
        androidUtils.passUserModelAsIntent(intent, user)
        startActivity(intent)
    }

    private fun removeMember(userID: String) {
        val id = groupID ?: return
        val member = membersList.firstOrNull { it.first.userID == userID }?.first
        val memberName = member?.username ?: getString(R.string.this_member)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.remove_member_title))
            .setMessage(getString(R.string.remove_group_member_message, memberName))
            .setPositiveButton(getString(R.string.remove_action)) { _, _ ->
                FirebaseGroups.getGroupReference(id)
                    .update("memberIDs", FieldValue.arrayRemove(userID))
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), getString(R.string.member_removed), Toast.LENGTH_SHORT)
                            .show()

                        val groupNameForNotification =
                            group?.groupName ?: getString(R.string.this_group)

                        FirebaseNotifications.createNotification(
                            type = "REMOVED_FROM_GROUP",
                            recipientID = userID,
                            senderID = FirebaseAuthentication.currentUserID() ?: "",
                            senderName = getString(R.string.admin_label),
                            groupID = id,
                            groupName = group?.groupName,
                            message = getString(
                                R.string.removed_from_group_notification,
                                groupNameForNotification
                            )
                        )

                        loadGroupDetails()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), getString(R.string.failed_to_remove_member), Toast.LENGTH_SHORT).show()
                    }
            }
                    .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun loadSharedContent() {
        val id = groupID ?: return
        FirebaseGroups.getGroupMessagesReference(id)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(500)
            .get()
            .addOnSuccessListener { snapshot ->
                sharedMediaItems.clear()
                sharedLinkItems.clear()
                sharedFileItems.clear()

                for (doc in snapshot.documents) {
                    val model = doc.toObject(GroupMsgModel::class.java) ?: continue
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
                Log.e("GroupSettings", "Failed to load shared content", e)
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

    companion object {
        fun newInstance(groupID: String): GroupSettingsFragment {
            return GroupSettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("groupID", groupID)
                }
            }
        }
    }

    private fun showEditGroupNameDialog() {
        val editText = EditText(requireContext()).apply {
            setText(groupName.text)
            setSingleLine()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_group_name))
            .setView(editText)
            .setPositiveButton(getString(R.string.saveBTN)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != group?.groupName) {
                    updateGroupName(newName)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show().apply {
                applyAccentToDialogButtons(this)
            }
    }

    private fun updateGroupName(newName: String) {
        FirebaseGroups.getGroupReference(groupID!!).update("groupName", newName)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.group_name_updated), Toast.LENGTH_SHORT).show()
                groupName.text = newName
                group?.groupName = newName
            }
            .addOnFailureListener { e ->
                Log.e("GroupSettings", "Failed to update name", e)
                Toast.makeText(requireContext(), getString(R.string.failed_to_update_name), Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateGroupImage(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            FirebaseGroups.getGroupReference(groupID!!).update("groupImage", base64)
                .addOnSuccessListener {
                    androidUtils.setProfileImageFromBase64(requireContext(), base64, groupImage)
                    Toast.makeText(requireContext(), getString(R.string.group_photo_updated), Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.failed_to_update_photo), Toast.LENGTH_SHORT)
                        .show()
                }
        } catch (e: Exception) {
            Log.e("GroupSettings", "Failed to update image", e)
            Toast.makeText(requireContext(), getString(R.string.failed_to_update_photo), Toast.LENGTH_SHORT).show()
        }
    }
}
