package com.example.smart_chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.user_chat.RecentChatRecyclerAdapter
import com.example.smart_chat.models.UserChatModel
import com.example.smart_chat.utils.firebase.FirebaseChat
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class DeletedChatsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecentChatRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_deleted_chats, container, false)

        recyclerView = view.findViewById(R.id.deleted_chats_recycler)

        setupRecycler()

        return view
    }

    private fun setupRecycler() {
        val query = FirebaseChat.getDeletedChatRoomsQuery()

        val options = FirestoreRecyclerOptions.Builder<UserChatModel>()
            .setQuery(query, UserChatModel::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = RecentChatRecyclerAdapter(options, requireActivity(), isDeletedView = true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
}
