package com.example.Smart_Chat.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.BlockedUsersAdapter
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils

class BlockedUsersFragment : Fragment() {

    private lateinit var blockedRecycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: BlockedUsersAdapter
    private val blockedUsersList = mutableListOf<userModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_blocked_users, container, false)

        blockedRecycler = view.findViewById(R.id.blocked_recycler)
        emptyState = view.findViewById(R.id.empty_state)

        setupRecyclerView()
        loadBlockedUsers()

        return view
    }

    private fun setupRecyclerView() {
        adapter = BlockedUsersAdapter(requireContext(), blockedUsersList)
        blockedRecycler.layoutManager = LinearLayoutManager(requireContext())
        blockedRecycler.adapter = adapter
    }

    private fun loadBlockedUsers() {
        blockedUsersList.clear()

        FireBase_utils.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val currentUser = document.toObject(userModel::class.java)
                val blockedIds = currentUser?.blockedUsers ?: emptyList()

                if (blockedIds.isEmpty()) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                var loadedCount = 0
                val totalBlocked = blockedIds.size

                blockedIds.forEach { blockedId ->
                    FireBase_utils.allUsersCollection().document(blockedId).get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                if (!blockedUsersList.any { it.userID == user.userID }) {
                                    blockedUsersList.add(user)
                                }
                            }

                            loadedCount++
                            if (loadedCount == totalBlocked) {
                                adapter.notifyDataSetChanged()
                                hideEmptyState()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("BlockedUsersFragment", "Failed to load user", e)
                            loadedCount++
                            if (loadedCount == totalBlocked) {
                                adapter.notifyDataSetChanged()
                                if (blockedUsersList.isEmpty()) {
                                    showEmptyState()
                                } else {
                                    hideEmptyState()
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BlockedUsersFragment", "Failed to load blocked users", e)
                showEmptyState()
            }
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        blockedRecycler.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        blockedRecycler.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadBlockedUsers()
    }
}