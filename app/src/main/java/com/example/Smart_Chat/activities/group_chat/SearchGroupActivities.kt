package com.example.Smart_Chat.activities.group_chat

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.SearchGroupRecyclerAdapter
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SearchGroupActivity : AppCompatActivity() {

    private lateinit var searchGroupInput: EditText
    private lateinit var searchGroupBTN: ImageButton
    private lateinit var groupList: RecyclerView
    private lateinit var backBTN: ImageButton
    private lateinit var emptyStateText: TextView

    private var adapter: SearchGroupRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_group)

        searchGroupInput = findViewById(R.id.searchGroupInput)
        searchGroupBTN = findViewById(R.id.searchGroupBTN)
        groupList = findViewById(R.id.groupList)
        backBTN = findViewById(R.id.back_btn)
        emptyStateText = findViewById(R.id.emptyStateText)

        searchGroupInput.requestFocus()

        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        searchGroupBTN.setOnClickListener {
            val query = searchGroupInput.text.toString()
            if (query.isEmpty()) {
                searchGroupInput.error = "Invalid group name"
                return@setOnClickListener
            }
            setupSearchRecyclerView(query)
        }
    }

    private fun setupSearchRecyclerView(searchQuery: String) {
        emptyStateText.visibility = View.GONE
        groupList.visibility = View.VISIBLE

        val query = FireBase_utils.allGroupsCollection()
            .whereGreaterThanOrEqualTo("groupName", searchQuery)
            .whereLessThanOrEqualTo("groupName", searchQuery + "\uf8ff")

        val options = FirestoreRecyclerOptions.Builder<groupModel>()
            .setQuery(query, groupModel::class.java)
            .build()

        adapter = SearchGroupRecyclerAdapter(options, this)
        groupList.layoutManager = LinearLayoutManager(this)
        groupList.adapter = adapter
        adapter?.startListening()

        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                checkEmpty()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                checkEmpty()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                checkEmpty()
            }

            private fun checkEmpty() {
                val isEmpty = adapter?.itemCount == 0
                emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
                groupList.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        })

        groupList.postDelayed({
            val isEmpty = adapter?.itemCount == 0
            emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            groupList.visibility = if (isEmpty) View.GONE else View.VISIBLE
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