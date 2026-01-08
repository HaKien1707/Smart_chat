package com.example.Smart_Chat.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.social.SearchUserRecyclerAdapter
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.Query

class SearchUserFragment : Fragment() {

    private lateinit var searchUserInput: EditText
    private lateinit var backBtn: ImageButton
    private lateinit var searchTabs: TabLayout
    private lateinit var peopleList: RecyclerView
    private lateinit var communitiesList: RecyclerView
    private lateinit var groupsList: RecyclerView
    private lateinit var emptyStateText: TextView

    private var peopleAdapter: SearchUserRecyclerAdapter? = null
    private var currentTabPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_user, container, false)

        searchUserInput = view.findViewById(R.id.searchUserInput)
        backBtn = view.findViewById(R.id.back_btn)
        searchTabs = view.findViewById(R.id.searchTabs)
        peopleList = view.findViewById(R.id.peopleList)
        communitiesList = view.findViewById(R.id.communitiesList)
        groupsList = view.findViewById(R.id.groupsList)
        emptyStateText = view.findViewById(R.id.emptyStateText)

        setupTabs()
        searchUserInput.requestFocus()

        backBtn.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        searchUserInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchTerm = s.toString()
                when (currentTabPosition) {
                    0 -> searchPeople(searchTerm)
                    1 -> searchCommunities(searchTerm)
                    2 -> searchGroups(searchTerm)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun setupTabs() {
        searchTabs.addTab(searchTabs.newTab().setText("People"))
        searchTabs.addTab(searchTabs.newTab().setText("Communities"))
        searchTabs.addTab(searchTabs.newTab().setText("Groups"))

        searchTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                showTabContent(currentTabPosition)
                
                // Re-trigger search with current query
                val searchTerm = searchUserInput.text.toString()
                when (currentTabPosition) {
                    0 -> searchPeople(searchTerm)
                    1 -> searchCommunities(searchTerm)
                    2 -> searchGroups(searchTerm)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showTabContent(position: Int) {
        peopleList.visibility = if (position == 0) View.VISIBLE else View.GONE
        communitiesList.visibility = if (position == 1) View.VISIBLE else View.GONE
        groupsList.visibility = if (position == 2) View.VISIBLE else View.GONE
    }

    private fun searchPeople(searchQuery: String) {
        if (searchQuery.isEmpty()) {
            peopleAdapter?.stopListening()
            peopleList.adapter = null
            emptyStateText.visibility = View.GONE
            return
        }

        val query = FirebaseAuthentication.allUsersCollection()
            .whereGreaterThanOrEqualTo("username", searchQuery)
            .whereLessThanOrEqualTo("username", searchQuery + "\uf8ff")

        val options = FirestoreRecyclerOptions.Builder<userModel>()
            .setQuery(query, userModel::class.java)
            .build()

        peopleAdapter = SearchUserRecyclerAdapter(options, requireActivity())
        peopleList.layoutManager = LinearLayoutManager(context)
        peopleList.adapter = peopleAdapter
        peopleAdapter?.startListening()

        peopleAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkEmpty()
            }
            private fun checkEmpty() {
                val isEmpty = peopleAdapter?.itemCount == 0
                emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
                peopleList.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        })
    }

    private fun searchCommunities(searchQuery: String) {
        // TODO: Implement communities search
        communitiesList.adapter = null
        emptyStateText.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
        emptyStateText.text = "Communities search coming soon"
    }

    private fun searchGroups(searchQuery: String) {
        // TODO: Implement groups search
        groupsList.adapter = null
        emptyStateText.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
        emptyStateText.text = "Groups search coming soon"
    }

    override fun onStart() {
        super.onStart()
        peopleAdapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        peopleAdapter?.stopListening()
    }
}
