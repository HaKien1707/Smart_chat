package com.example.smart_chat.fragment

import android.graphics.Color
import android.os.Bundle
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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.group.GroupMemberAdapter
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseGroups
import com.example.smart_chat.utils.others.androidUtils
import com.google.android.material.tabs.TabLayout

class GroupSettingsFragment : Fragment() {

    private lateinit var backBtn: ImageButton
    private lateinit var editBtn: ImageButton
    private lateinit var moreBtn: ImageButton
    private lateinit var groupImage: ImageView
    private lateinit var groupName: TextView
    private lateinit var membersCount: TextView
    private lateinit var messageBtn: LinearLayout
    private lateinit var muteBtn: LinearLayout
    private lateinit var leaveBtn: LinearLayout
    private lateinit var addMembersBtn: LinearLayout
    private lateinit var tabs: TabLayout
    private lateinit var membersRecycler: RecyclerView

    private var groupID: String? = null
    private var group: groupModel? = null
    private val membersList = mutableListOf<Pair<userModel, Boolean>>()
    private var adapter: GroupMemberAdapter? = null
    private var currentUserIsAdmin = false

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
        loadMembers()
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
        leaveBtn = view.findViewById(R.id.leave_btn)
        addMembersBtn = view.findViewById(R.id.add_members_btn)
        tabs = view.findViewById(R.id.tabs)
        membersRecycler = view.findViewById(R.id.members_recycler)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            activity?.finish()
        }

        editBtn.setOnClickListener {
            showEditGroupNameDialog()
        }

        moreBtn.setOnClickListener {
            // TODO: Show more options menu
            Toast.makeText(requireContext(), "More options", Toast.LENGTH_SHORT).show()
        }

        messageBtn.setOnClickListener {
            // Go back to group chat
            activity?.finish()
        }

        muteBtn.setOnClickListener {
            // TODO: Implement mute functionality
            Toast.makeText(requireContext(), "Mute notifications", Toast.LENGTH_SHORT).show()
        }

        leaveBtn.setOnClickListener {
            // TODO: Implement leave group
            Toast.makeText(requireContext(), "Leave group", Toast.LENGTH_SHORT).show()
        }

        addMembersBtn.setOnClickListener {
            // TODO: Implement add members
            Toast.makeText(requireContext(), "Add members", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        tabs.addTab(tabs.newTab().setText("Members"))
        tabs.addTab(tabs.newTab().setText("Media"))
        tabs.addTab(tabs.newTab().setText("Links"))
        tabs.addTab(tabs.newTab().setText("Voice"))

        // Set default visibility for members tab
        membersRecycler.visibility = View.VISIBLE

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Show members
                        membersRecycler.visibility = View.VISIBLE
                    }
                    1 -> {
                        // Show media
                        membersRecycler.visibility = View.GONE
                        Toast.makeText(requireContext(), "Media - Coming soon", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        // Show links
                        membersRecycler.visibility = View.GONE
                        Toast.makeText(requireContext(), "Links - Coming soon", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        // Show voice
                        membersRecycler.visibility = View.GONE
                        Toast.makeText(requireContext(), "Voice - Coming soon", Toast.LENGTH_SHORT).show()
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
        adapter = GroupMemberAdapter(
            members = membersList,
            context = requireContext(),
            currentUserIsAdmin = currentUserIsAdmin,
            currentUserID = FirebaseAuthentication.currentUserID(),
            onMemberClick = { user ->
                // TODO: Handle member click
            },
            onRemoveMember = { userID ->
                // TODO: Handle remove member
            },
            onBlockMember = { userID ->
                // TODO: Handle block member
            }
        )
        membersRecycler.layoutManager = LinearLayoutManager(requireContext())
        membersRecycler.adapter = adapter
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

                // Check if current user is admin
                currentUserIsAdmin = group?.adminIDs?.contains(FirebaseAuthentication.currentUserID()) == true
            }
            .addOnFailureListener { e ->
                Log.e("GroupSettings", "Failed to load group", e)
                Toast.makeText(requireContext(), "Failed to load group", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMembers() {
        // Load all users from Firestore
        FirebaseAuthentication.allUsersCollection().get()
            .addOnSuccessListener { documents ->
                membersList.clear()
                var totalMembers = 0

                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    if (user != null) {
                        val isAdmin = group?.adminIDs?.contains(user.userID) == true
                        membersList.add(Pair(user, isAdmin))
                        totalMembers++
                    }
                }

                // Sort: owner first, then others
                membersList.sortWith(compareBy {
                    if (it.first.userID == group?.adminIDs?.getOrNull(0)) 0 else 1
                })

                membersCount.text = "$totalMembers members"
                adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("GroupSettings", "Failed to load members", e)
                Toast.makeText(requireContext(), "Failed to load members", Toast.LENGTH_SHORT).show()
            }
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
            .setTitle("Edit Group Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != group?.groupName) {
                    updateGroupName(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
            }
    }

    private fun updateGroupName(newName: String) {
        FirebaseGroups.getGroupReference(groupID!!).update("groupName", newName)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Group name updated", Toast.LENGTH_SHORT).show()
                groupName.text = newName
                group?.groupName = newName
            }
            .addOnFailureListener { e ->
                Log.e("GroupSettings", "Failed to update name", e)
                Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
            }
    }
}
