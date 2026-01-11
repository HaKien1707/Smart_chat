package com.example.smart_chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import android.content.Intent
import com.example.smart_chat.adapters.temporary_chat.TemporaryChatAdapter
import com.example.smart_chat.activities.temporary_chat.CreateTemporaryChatActivity
import com.example.smart_chat.models.temp_chat.TemporaryChatModel
import com.example.smart_chat.utils.firebase.FirebaseTemporaryChat
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TemporaryChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: TemporaryChatAdapter? = null
    private lateinit var fabAddTempChat: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_temporary_chat, container, false)

        recyclerView = view.findViewById(R.id.temp_chat_recycler)
        fabAddTempChat = view.findViewById(R.id.fab_add_temp_chat)

        fabAddTempChat.setOnClickListener {
            startActivity(Intent(requireContext(), CreateTemporaryChatActivity::class.java))
        }

        setupRecycler()

        // Clean up expired chats
        FirebaseTemporaryChat.deleteExpiredTemporaryChats()

        return view
    }

    private fun setupRecycler() {
        val query = FirebaseTemporaryChat.getUserTemporaryChatsQuery()

        val options = FirestoreRecyclerOptions.Builder<TemporaryChatModel>()
            .setQuery(query, TemporaryChatModel::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = TemporaryChatAdapter(options, requireContext())

        // Disable animations to prevent inconsistency
        try {
            val animator = recyclerView.itemAnimator
            if (animator is androidx.recyclerview.widget.SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            recyclerView.itemAnimator = null
        } catch (e: Exception) {
            android.util.Log.w("TemporaryChatFragment", "Failed to disable animations: ${e.message}")
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onResume() {
        super.onResume()
        // Clean up expired chats when fragment resumes
        FirebaseTemporaryChat.deleteExpiredTemporaryChats()
    }
}