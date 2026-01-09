package com.example.smart_chat.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.social.BlockedUsersAdapter
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.firebase.FirebaseAuthentication

class BlockedUsersFragment : Fragment() {

    private lateinit var blockedRecycler: RecyclerView
    private lateinit var adapter: BlockedUsersAdapter
    private val blockedUsersList = mutableListOf<userModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_blocked_users, container, false)

        blockedRecycler = view.findViewById(R.id.blocked_recycler)

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

        FirebaseAuthentication.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val currentUser = document.toObject(userModel::class.java)
                val blockedIds = currentUser?.blockedUsers?.filterNotNull() ?: emptyList()

                if (blockedIds.isEmpty()) {
                    return@addOnSuccessListener
                }

                var loadedCount = 0
                val totalBlocked = blockedIds.size

                blockedIds.forEach { blockedId ->
                    FirebaseAuthentication.allUsersCollection().document(blockedId).get()
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
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("BlockedUsersFragment", "Failed to load user", e)
                            loadedCount++
                            if (loadedCount == totalBlocked) {
                                adapter.notifyDataSetChanged()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BlockedUsersFragment", "Failed to load blocked users", e)
            }
    }

    private fun showEmptyState() {
        blockedRecycler.visibility = View.GONE
    }

    private fun hideEmptyState() {
        blockedRecycler.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadBlockedUsers()
    }
}