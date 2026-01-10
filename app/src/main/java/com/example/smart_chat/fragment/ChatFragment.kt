package com.example.smart_chat.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.activities.community.CreateCommunityActivity
import com.example.smart_chat.activities.group_chat.CreateGroupMembersActivity
import com.example.smart_chat.R
import com.example.smart_chat.adapters.common.UnifiedChatItem
import com.example.smart_chat.adapters.common.UnifiedChatListAdapter
import com.example.smart_chat.models.UserChatModel
import com.example.smart_chat.models.community.CommunityModel
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseChat
import com.example.smart_chat.utils.firebase.FirebaseCommunity
import com.example.smart_chat.utils.firebase.FirebaseGroups
import com.google.firebase.firestore.Query
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ListenerRegistration

class ChatFragment : Fragment() {

    private lateinit var chatRecycler: RecyclerView
    private lateinit var fabCreateGroup: FloatingActionButton

    private lateinit var adapter: UnifiedChatListAdapter

    private var userChatsListener: ListenerRegistration? = null
    private var communitiesListener: ListenerRegistration? = null
    private var groupsListener: ListenerRegistration? = null

    private var userChats: List<UserChatModel> = emptyList()
    private var communities: List<CommunityModel> = emptyList()
    private var groups: List<groupModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        chatRecycler = view.findViewById(R.id.chatRecycler)
        fabCreateGroup = view.findViewById(R.id.fab_create_group)

        setupRecyclerView()

        fabCreateGroup.setOnClickListener {
            val menu = PopupMenu(requireContext(), fabCreateGroup)
            menu.menuInflater.inflate(R.menu.menu_fab_create, menu.menu)
            menu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_create_group -> {
                        startActivity(Intent(requireContext(), CreateGroupMembersActivity::class.java))
                        true
                    }
                    R.id.action_create_community -> {
                        startActivity(Intent(requireContext(), CreateCommunityActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            menu.show()
        }

        return view
    }

    private fun setupRecyclerView() {
        adapter = UnifiedChatListAdapter(requireContext())
        chatRecycler.layoutManager = LinearLayoutManager(requireContext())
        chatRecycler.itemAnimator = null
        chatRecycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        startListeners()
    }

    override fun onStop() {
        super.onStop()
        stopListeners()
    }

    override fun onResume() {
        super.onResume()
        // no-op; snapshots will drive updates
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatRecycler.adapter = null
        stopListeners()
    }

    private fun startListeners() {
        if (userChatsListener != null || communitiesListener != null || groupsListener != null) return

        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

        val userChatsQuery = FirebaseChat.allChatRoomsCollection()
            .whereArrayContains("userID", currentUserID)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

        userChatsListener = userChatsQuery.addSnapshotListener { snapshot, _ ->
            val items = snapshot?.documents
                ?.mapNotNull { doc ->
                    doc.toObject(UserChatModel::class.java)?.apply {
                        if (chatRoomID.isNullOrBlank()) chatRoomID = doc.id
                    }?.takeIf { chat ->
                        !chat.deletedBy.contains(currentUserID)
                    }
                }
                ?: emptyList()

            userChats = items
            rebuildMergedList()
        }

        val communitiesQuery = FirebaseCommunity.allCommunitiesCollection()
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

        communitiesListener = communitiesQuery.addSnapshotListener { snapshot, _ ->
            val items = snapshot?.documents
                ?.mapNotNull { doc ->
                    doc.toObject(CommunityModel::class.java)?.apply {
                        if (communityID.isNullOrBlank()) communityID = doc.id
                    }
                }
                ?: emptyList()

            communities = items
            rebuildMergedList()
        }

        val groupsQuery = FirebaseGroups.getUserGroupsQuery()

        groupsListener = groupsQuery.addSnapshotListener { snapshot, _ ->
            val items = snapshot?.documents
                ?.mapNotNull { doc ->
                    doc.toObject(groupModel::class.java)?.apply {
                        if (groupID.isNullOrBlank()) groupID = doc.id
                    }
                }
                ?: emptyList()

            groups = items
            rebuildMergedList()
        }
    }

    private fun stopListeners() {
        userChatsListener?.remove(); userChatsListener = null
        communitiesListener?.remove(); communitiesListener = null
        groupsListener?.remove(); groupsListener = null
    }

    private fun rebuildMergedList() {
        val merged = ArrayList<UnifiedChatItem>(userChats.size + communities.size + groups.size)

        for (chat in userChats) {
            val id = chat.chatRoomID ?: continue
            val ts = chat.lastMsgTimestamp?.toDate()?.time ?: 0L
            merged.add(UnifiedChatItem.UserChat(id = id, sortTimestampMs = ts, model = chat))
        }

        for (community in communities) {
            val id = community.communityID ?: continue
            val ts = community.lastMsgTimestamp?.toDate()?.time
                ?: community.createdTimestamp?.toDate()?.time
                ?: 0L
            merged.add(UnifiedChatItem.Community(id = id, sortTimestampMs = ts, model = community))
        }

        for (group in groups) {
            val id = group.groupID ?: continue
            val ts = group.lastMsgTimestamp?.toDate()?.time
                ?: group.createdTimestamp?.toDate()?.time
                ?: 0L
            merged.add(UnifiedChatItem.Group(id = id, sortTimestampMs = ts, model = group))
        }

        merged.sortByDescending { it.sortTimestampMs }
        adapter.submitList(merged)
    }
}