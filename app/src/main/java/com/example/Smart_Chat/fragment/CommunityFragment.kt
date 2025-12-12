package com.example.Smart_Chat.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.CommunityAdapter
import com.example.Smart_Chat.models.CommunityModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query

class CommunityFragment : Fragment() {

    private var communityRecycler: RecyclerView? = null
    private var adapter: CommunityAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_community, container, false)

        communityRecycler = view.findViewById(R.id.community_recycler)

        setupCommunityRecycler()

        return view
    }

    private fun setupCommunityRecycler() {
        val query = FireBase_utils.allCommunitiesCollection()
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<CommunityModel>()
            .setQuery(query, CommunityModel::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = CommunityAdapter(options, requireContext())

        // Disable item animator to prevent crashes
        try {
            communityRecycler?.itemAnimator?.let { animator ->
                if (animator is androidx.recyclerview.widget.SimpleItemAnimator) {
                    animator.supportsChangeAnimations = false
                }
            }
            communityRecycler?.itemAnimator = null
        } catch (e: Exception) {
            Log.w("CommunityFragment", "Failed to modify itemAnimator: ${e.message}")
        }

        communityRecycler?.layoutManager = LinearLayoutManager(requireContext())
        communityRecycler?.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter?.stopListening()
        communityRecycler?.adapter = null
        adapter = null
        communityRecycler = null
    }
}