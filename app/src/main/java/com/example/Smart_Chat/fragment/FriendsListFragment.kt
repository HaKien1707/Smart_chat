package com.example.Smart_Chat.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.FriendsListAdapter
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils

class FriendsListFragment : Fragment() {

    private lateinit var friendsRecycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyText: TextView
    private lateinit var viewBlockedUsersBtn: Button // NEW
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
        viewBlockedUsersBtn = view.findViewById(R.id.view_blocked_users_btn) // NEW

        setupRecyclerView()
        loadFriends()

        // NEW: Click to view blocked users
        viewBlockedUsersBtn.setOnClickListener {
            openBlockedUsers()
        }

        return view
    }

    private fun openBlockedUsers() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frame, BlockedUsersFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun setupRecyclerView() {
        adapter = FriendsListAdapter(requireContext(), friendsList)
        friendsRecycler.layoutManager = LinearLayoutManager(requireContext())
        friendsRecycler.adapter = adapter
    }

    private fun loadFriends() {
        friendsList.clear()

        FireBase_utils.getAllFriends(
            onSuccess = { friendIds ->
                if (friendIds.isEmpty()) {
                    showEmptyState()
                    return@getAllFriends
                }

                var loadedCount = 0
                val totalFriends = friendIds.size

                friendIds.forEach { friendId ->
                    FireBase_utils.allUsersCollection().document(friendId).get()
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