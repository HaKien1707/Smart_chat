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
import com.google.firebase.firestore.Query

class SearchUserFragment : Fragment() {

    private lateinit var searchUserInput: EditText
    private lateinit var backBtn: ImageButton
    private lateinit var userList: RecyclerView
    private lateinit var emptyStateText: TextView

    private var adapter: SearchUserRecyclerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_user, container, false)

        searchUserInput = view.findViewById(R.id.searchUserInput)
        backBtn = view.findViewById(R.id.back_btn)
        userList = view.findViewById(R.id.userList)
        emptyStateText = view.findViewById(R.id.emptyStateText)

        searchUserInput.requestFocus()

        backBtn.setOnClickListener {
            // Use the activity's back press dispatcher to ensure consistent behavior
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        searchUserInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchTerm = s.toString()
                if (searchTerm.isNotEmpty()) {
                    setupSearchRecyclerView(searchTerm)
                } else {
                    adapter?.stopListening()
                    userList.adapter = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun setupSearchRecyclerView(searchQuery: String) {
        val query = FirebaseAuthentication.allUsersCollection()
            .whereGreaterThanOrEqualTo("username", searchQuery)
            .whereLessThanOrEqualTo("username", searchQuery + "\uf8ff")

        val options = FirestoreRecyclerOptions.Builder<userModel>()
            .setQuery(query, userModel::class.java)
            .build()

        adapter = SearchUserRecyclerAdapter(options, requireActivity())
        userList.layoutManager = LinearLayoutManager(context)
        userList.adapter = adapter
        adapter?.startListening()

        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkEmpty()
            }
            private fun checkEmpty() {
                val isEmpty = adapter?.itemCount == 0
                emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
                userList.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        })
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }
}
