package com.example.smart_chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.activities.community.CreateCommunityActivity
import com.example.smart_chat.activities.group_chat.CreateGroupMembersActivity
import com.example.smart_chat.adapters.group.GroupRecyclerAdapter
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.utils.firebase.FirebaseGroups
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GroupFragment : Fragment() {

    private lateinit var groupRecycler: RecyclerView
    private lateinit var emptyGroupText: TextView
    private lateinit var fabCreateGroup: FloatingActionButton
    private var adapter: GroupRecyclerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)

        groupRecycler = view.findViewById(R.id.groupRecycler)
        emptyGroupText = view.findViewById(R.id.emptyGroupText)
        fabCreateGroup = view.findViewById(R.id.fab_create_group)

        // Click listener for FAB
        fabCreateGroup.setOnClickListener {
            val menu = PopupMenu(requireContext(), fabCreateGroup)
            menu.menuInflater.inflate(R.menu.menu_fab_create, menu.menu)
            menu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_create_group -> {
                        startActivity(android.content.Intent(requireContext(), CreateGroupMembersActivity::class.java))
                        true
                    }
                    R.id.action_create_community -> {
                        startActivity(android.content.Intent(requireContext(), CreateCommunityActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            menu.show()
        }

        setupGroupRecyclerView()

        return view
    }

    private fun setupGroupRecyclerView() {
        val query = FirebaseGroups.getUserGroupsQuery()

        val options = FirestoreRecyclerOptions.Builder<groupModel>()
            .setQuery(query, groupModel::class.java)
            .build()

        adapter = GroupRecyclerAdapter(options, requireContext())
        groupRecycler.layoutManager = LinearLayoutManager(requireContext())
        groupRecycler.adapter = adapter
        adapter?.startListening()

        // Show/hide empty state
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkEmpty()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                checkEmpty()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                checkEmpty()
            }

            private fun checkEmpty() {
                val isEmpty = adapter?.itemCount == 0
                emptyGroupText.visibility = if (isEmpty) View.VISIBLE else View.GONE
                groupRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        })
    }

    // create flow handled via popup menu

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

    companion object {
        fun newInstance() = GroupFragment()
    }
}