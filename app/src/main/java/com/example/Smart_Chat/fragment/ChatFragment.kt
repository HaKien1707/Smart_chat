package com.example.Smart_Chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.adapters.RecentChatRecyclerAdapter
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.chatRoomModel
import com.example.Smart_Chat.utils.FireBase_utils.allChatRoomsCollectionReference
import com.example.Smart_Chat.utils.FireBase_utils.currentUserID
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query

class ChatFragment : Fragment() {

    private lateinit var chatRecycler: RecyclerView
    private var adapter: RecentChatRecyclerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        chatRecycler = view.findViewById(R.id.chatRecycler)

        setupRecentChatRecyclerView()

        return view
    }

    private fun setupRecentChatRecyclerView(searchQuery: String = "") {
        val query = allChatRoomsCollectionReference()
            .whereArrayContains("userID", currentUserID()!!)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<chatRoomModel>()
            .setQuery(query, chatRoomModel::class.java)
            .build()

        adapter = RecentChatRecyclerAdapter(options, requireContext())
        chatRecycler.layoutManager = LinearLayoutManager(requireContext())
        chatRecycler.adapter = adapter
        adapter?.startListening()
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
        adapter?.notifyDataSetChanged()
    }
}