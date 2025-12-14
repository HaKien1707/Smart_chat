package com.example.Smart_Chat.activities.user_chat

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.SearchUserRecyclerAdapter
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SearchUserActivity : AppCompatActivity() {

    private lateinit var searchUserInput: EditText
    private lateinit var searchUserBTN: ImageButton
    private lateinit var userList: RecyclerView
    private lateinit var backBTN: ImageButton
    private lateinit var emptyStateText: TextView

    private var adapter: SearchUserRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)

        searchUserInput = findViewById(R.id.searchUserInput)
        searchUserBTN = findViewById(R.id.searchUserBTN)
        userList = findViewById(R.id.userList)
        backBTN = findViewById(R.id.back_btn)
        emptyStateText = findViewById(R.id.emptyStateText)

        searchUserInput.requestFocus()

        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        searchUserBTN.setOnClickListener {
            val query = searchUserInput.text.toString()
            if (query.isEmpty()) {
                searchUserInput.error = "Invalid username"
                return@setOnClickListener
            }
            setupSearchRecyclerView(query)
        }
    }

    private fun setupSearchRecyclerView(searchQuery: String) {
        // First, show empty state initially
        emptyStateText.visibility = View.GONE
        userList.visibility = View.VISIBLE

        val query = FireBase_utils.allUsersCollection()
            .whereGreaterThanOrEqualTo("username", searchQuery)
            .whereLessThanOrEqualTo("username", searchQuery + "\uf8ff")

        val options = FirestoreRecyclerOptions.Builder<userModel>()
            .setQuery(query, userModel::class.java)
            .build()

        adapter = SearchUserRecyclerAdapter(options, this)
        userList.layoutManager = LinearLayoutManager(this)
        userList.adapter = adapter
        adapter?.startListening()

        // Show/hide empty state based on results
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
                emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
                userList.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        })

        // Trigger initial check after a short delay to let adapter load
        userList.postDelayed({
            val isEmpty = adapter?.itemCount == 0
            emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            userList.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }, 500)
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