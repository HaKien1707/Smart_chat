package com.example.Smart_Chat.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.user_chat.FriendsListAdapter
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.androidUtils
import com.example.Smart_Chat.utils.firebase.*

class FriendsListFragment : Fragment() {

    private lateinit var friendsRecycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyText: TextView
    private lateinit var viewBlockedUsersBtn: Button
    private lateinit var adapter: FriendsListAdapter
    private val friendsList = mutableListOf<userModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friends_list, container, false)

        friendsRecycler = view.findViewById(R.id.friends_recycler)
        emptyState = view.findViewById(R.id.empty_state)
        emptyText = view.findViewById(R.id.empty_text)
        viewBlockedUsersBtn = view.findViewById(R.id.view_blocked_users_btn)

        setupRecyclerView()
        loadFriends()

        // Click to view blocked users
        viewBlockedUsersBtn.setOnClickListener {
            openBlockedUsers()
        }

        return view
    }

    private fun openBlockedUsers() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_blocked_user, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.blocked_users_recycler)
        val unblockButton = dialogView.findViewById<Button>(R.id.unblock_button)
        val emptyState = dialogView.findViewById<TextView>(R.id.empty_blocked_state)
        val selectAllCheckbox = dialogView.findViewById<CheckBox>(R.id.select_all_checkbox)

        val blockedUsers = mutableListOf<userModel>()
        val selectedUsers = mutableSetOf<String>()

        // Setup adapter with checkboxes
        val adapter = BlockedUsersCheckboxAdapter(
            blockedUsers,
            onCheckChanged = { userId, isChecked ->
                if (isChecked) {
                    selectedUsers.add(userId)
                } else {
                    selectedUsers.remove(userId)
                }

                // Update select all checkbox state
                selectAllCheckbox.isChecked = selectedUsers.size == blockedUsers.size

                // Enable/disable button based on selection
                updateUnblockButton(unblockButton, selectedUsers.isNotEmpty())
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Select All functionality
        selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            adapter.selectAll(isChecked)
            selectedUsers.clear()
            if (isChecked) {
                selectedUsers.addAll(blockedUsers.mapNotNull { it.userID })
            }
            updateUnblockButton(unblockButton, selectedUsers.isNotEmpty())
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Load blocked users
        loadBlockedUsers { users ->
            blockedUsers.clear()
            blockedUsers.addAll(users)
            adapter.notifyDataSetChanged()

            // Show/hide views based on blocked users
            if (users.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                unblockButton.visibility = View.GONE
                selectAllCheckbox.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                unblockButton.visibility = View.VISIBLE
                selectAllCheckbox.visibility = View.VISIBLE
            }
        }

        unblockButton.setOnClickListener {
            if (selectedUsers.isEmpty()) {
                Toast.makeText(requireContext(), "No users selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show confirmation dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Unblock Users")
                .setMessage("Are you sure you want to unblock ${selectedUsers.size} user(s)?")
                .setPositiveButton("Unblock") { _, _ ->
                    unblockMultipleUsers(selectedUsers.toList()) {
                        Toast.makeText(requireContext(), "Users unblocked", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadFriends()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    // Helper function to enable/disable unblock button with visual feedback
    private fun updateUnblockButton(button: Button, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1.0f else 0.5f

        // Optional: Change background tint
        if (enabled) {
            button.backgroundTintList = requireContext().getColorStateList(R.color.violet)
        } else {
            button.backgroundTintList = requireContext().getColorStateList(android.R.color.darker_gray)
        }
    }

    private fun loadBlockedUsers(onComplete: (List<userModel>) -> Unit) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

        FirebaseAuthentication.allUsersCollection()
            .document(currentUserID)
            .get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(userModel::class.java)
                val blockedIDs = user?.blockedUsers ?: emptyList()

                if (blockedIDs.isEmpty()) {
                    onComplete(emptyList())
                    return@addOnSuccessListener
                }

                // Load user details for each blocked ID
                val blockedUsers = mutableListOf<userModel>()
                var loadedCount = 0

                blockedIDs.forEach { blockedID ->
                    FirebaseAuthentication.allUsersCollection()
                        .document(blockedID.toString())
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val blockedUser = userDoc.toObject(userModel::class.java)
                            if (blockedUser != null) {
                                blockedUsers.add(blockedUser)
                            }

                            loadedCount++
                            if (loadedCount == blockedIDs.size) {
                                onComplete(blockedUsers)
                            }
                        }
                        .addOnFailureListener {
                            loadedCount++
                            if (loadedCount == blockedIDs.size) {
                                onComplete(blockedUsers)
                            }
                        }
                }
            }
    }

    private fun unblockMultipleUsers(userIDs: List<String>, onComplete: () -> Unit) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

        FirebaseAuthentication.allUsersCollection()
            .document(currentUserID)
            .get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(userModel::class.java)
                val currentBlocked = user?.blockedUsers?.toMutableList() ?: mutableListOf()

                // Remove all selected users
                currentBlocked.removeAll(userIDs)

                // Update Firestore with correct field name
                FirebaseAuthentication.allUsersCollection()
                    .document(currentUserID)
                    .update("blockedUsers", currentBlocked) // Changed from blockedUserIDs
                    .addOnSuccessListener {
                        Log.d("FriendsListFragment", "Successfully unblocked ${userIDs.size} users")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FriendsListFragment", "Failed to unblock users", e)
                        Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FriendsListFragment", "Failed to get user data", e)
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    class BlockedUsersCheckboxAdapter(
        private val users: List<userModel>,
        private val onCheckChanged: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<BlockedUsersCheckboxAdapter.ViewHolder>() {

        private val checkedStates = mutableMapOf<String, Boolean>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blocked_user_checkbox, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.username.text = user.username

            if (!user.profileImage.isNullOrBlank()) {
                androidUtils.setProfileImageFromBase64(
                    holder.itemView.context,
                    user.profileImage,
                    holder.profileImage
                )
            } else {
                holder.profileImage.setImageResource(R.drawable.ic_profile)
            }

            // Remove listener before setting state to avoid triggering callback
            holder.checkbox.setOnCheckedChangeListener(null)
            holder.checkbox.isChecked = checkedStates[user.userID] ?: false

            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                checkedStates[user.userID ?: ""] = isChecked
                onCheckChanged(user.userID ?: "", isChecked)
            }
        }

        override fun getItemCount() = users.size

        // Select/deselect all users
        fun selectAll(select: Boolean) {
            users.forEach { user ->
                checkedStates[user.userID ?: ""] = select
            }
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val profileImage: ImageView = view.findViewById(R.id.profile_image)
            val username: TextView = view.findViewById(R.id.username)
            val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        }
    }

    private fun setupRecyclerView() {
        adapter = FriendsListAdapter(requireContext(), friendsList)
        friendsRecycler.layoutManager = LinearLayoutManager(requireContext())
        friendsRecycler.adapter = adapter
    }

    private fun loadFriends() {
        friendsList.clear()

        FirebaseFriends.getAllFriends(
            onSuccess = { friendIds ->
                if (friendIds.isEmpty()) {
                    showEmptyState()
                    return@getAllFriends
                }

                var loadedCount = 0
                val totalFriends = friendIds.size

                friendIds.forEach { friendId ->
                    FirebaseAuthentication.allUsersCollection().document(friendId).get()
                        .addOnSuccessListener { friendDoc ->
                            val friend = friendDoc.toObject(userModel::class.java)
                            if (friend != null) {
                                if (!friendsList.any { it.userID == friend.userID }) {
                                    friendsList.add(friend)
                                }
                            }

                            loadedCount++

                            if (loadedCount == totalFriends) {
                                adapter.notifyDataSetChanged()
                                hideEmptyState()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FriendsListFragment", "Failed to load friend", e)
                            loadedCount++

                            if (loadedCount == totalFriends) {
                                adapter.notifyDataSetChanged()
                                if (friendsList.isEmpty()) {
                                    showEmptyState()
                                } else {
                                    hideEmptyState()
                                }
                            }
                        }
                }
            },
            onFailure = { e ->
                Log.e("FriendsListFragment", "Failed to load friends", e)
                showEmptyState()
            }
        )
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        friendsRecycler.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        friendsRecycler.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadFriends()
    }
}