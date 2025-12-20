package com.example.Smart_Chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.temporary_chat.TemporaryChatAdapter
import com.example.Smart_Chat.models.temp_chat.TemporaryChatModel
import com.example.Smart_Chat.utils.firebase.FirebaseTemporaryChat
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class TemporaryChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: TemporaryChatAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_temporary_chat, container, false)

        recyclerView = view.findViewById(R.id.temp_chat_recycler)

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