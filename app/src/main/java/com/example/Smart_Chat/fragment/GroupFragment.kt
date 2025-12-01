package com.example.Smart_Chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.CreateGroupActivity
import com.example.Smart_Chat.adapters.GroupRecyclerAdapter
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.utils.FireBase_utils
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
            showCreateGroupDialog()
        }

        setupGroupRecyclerView()

        return view
    }

    private fun setupGroupRecyclerView() {
        val query = FireBase_utils.getUserGroupsQuery()

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

    private fun showCreateGroupDialog() {
        val intent = android.content.Intent(requireContext(), CreateGroupActivity::class.java)
        startActivity(intent)
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

    companion object {
        fun newInstance() = GroupFragment()
    }
}