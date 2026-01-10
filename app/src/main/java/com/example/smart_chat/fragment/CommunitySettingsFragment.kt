package com.example.smart_chat.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.community.CommunityMemberAdapter
import com.example.smart_chat.models.community.CommunityModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseCommunity
import com.example.smart_chat.utils.others.androidUtils
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.tabs.TabLayout
import java.io.ByteArrayOutputStream

class CommunitySettingsFragment : Fragment() {

    private lateinit var backBtn: ImageButton
    private lateinit var editBtn: ImageButton
    private lateinit var moreBtn: ImageButton
    private lateinit var communityImage: ImageView
    private lateinit var communityName: TextView
    private lateinit var membersCount: TextView
    private lateinit var messageBtn: LinearLayout
    private lateinit var muteBtn: LinearLayout
    private lateinit var leaveBtn: LinearLayout
    private lateinit var descriptionValue: TextView
    private lateinit var inviteLinkValue: TextView
    private lateinit var tabs: TabLayout
    private lateinit var membersRecycler: RecyclerView

    private var communityID: String? = null
    private var community: CommunityModel? = null
    private val membersList = mutableListOf<userModel>()
    private var adapter: CommunityMemberAdapter? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    updateCommunityImage(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            communityID = it.getString("communityID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (communityID == null) {
            activity?.finish()
            return
        }

        initViews(view)
        setupListeners()
        setupTabs()
        setupRecycler()
        loadCommunityDetails()
        loadMembers()
    }

    private fun initViews(view: View) {
        backBtn = view.findViewById(R.id.back_btn)
        editBtn = view.findViewById(R.id.edit_btn)
        moreBtn = view.findViewById(R.id.more_btn)
        communityImage = view.findViewById(R.id.community_image)
        communityName = view.findViewById(R.id.community_name)
        membersCount = view.findViewById(R.id.members_count)
        messageBtn = view.findViewById(R.id.message_btn)
        muteBtn = view.findViewById(R.id.mute_btn)
        leaveBtn = view.findViewById(R.id.leave_btn)
        descriptionValue = view.findViewById(R.id.description_value)
        inviteLinkValue = view.findViewById(R.id.invite_link_value)
        tabs = view.findViewById(R.id.tabs)
        membersRecycler = view.findViewById(R.id.members_recycler)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            activity?.finish()
        }

        communityImage.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(512)
                .maxResultSize(512, 512)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }

        editBtn.setOnClickListener {
            showEditCommunityNameDialog()
        }

        moreBtn.setOnClickListener {
            // TODO: Show more options menu
            Toast.makeText(requireContext(), "More options", Toast.LENGTH_SHORT).show()
        }

        messageBtn.setOnClickListener {
            // Go back to community chat
            activity?.finish()
        }

        muteBtn.setOnClickListener {
            // TODO: Implement mute functionality
            Toast.makeText(requireContext(), "Mute notifications", Toast.LENGTH_SHORT).show()
        }

        leaveBtn.setOnClickListener {
            // TODO: Implement leave community
            Toast.makeText(requireContext(), "Leave community", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        tabs.addTab(tabs.newTab().setText("Members"))
        tabs.addTab(tabs.newTab().setText("Media"))
        tabs.addTab(tabs.newTab().setText("Links"))
        tabs.addTab(tabs.newTab().setText("Files"))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Show members
                        membersRecycler.visibility = View.VISIBLE
                    }
                    1 -> {
                        // Show media
                        membersRecycler.visibility = View.GONE
                        Toast.makeText(requireContext(), "Media - Coming soon", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        // Show links
                        membersRecycler.visibility = View.GONE
                        Toast.makeText(requireContext(), "Links - Coming soon", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        // Show files
                        membersRecycler.visibility = View.GONE
                        Toast.makeText(requireContext(), "Files - Coming soon", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecycler() {
        adapter = CommunityMemberAdapter(requireContext(), membersList, community?.adminID)
        membersRecycler.layoutManager = LinearLayoutManager(requireContext())
        membersRecycler.adapter = adapter
    }

    private fun loadCommunityDetails() {
        FirebaseCommunity.getCommunityReference(communityID!!).get()
            .addOnSuccessListener { document ->
                community = document.toObject(CommunityModel::class.java)

                communityName.text = community?.communityName ?: "Community"

                descriptionValue.text = community?.communityDescription ?: ""
                inviteLinkValue.text = "smartchat://community/${communityID ?: ""}"

                // Load community image
                val imageUrl = community?.communityImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(requireContext(), imageUrl, communityImage)
                } else {
                    communityImage.setImageResource(R.drawable.ic_community)
                }

                // Update adapter with admin ID
                adapter?.updateAdminID(community?.adminID)
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to load community", e)
                Toast.makeText(requireContext(), "Failed to load community", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMembers() {
        // Load all users from Firestore
        FirebaseAuthentication.allUsersCollection().get()
            .addOnSuccessListener { documents ->
                membersList.clear()
                var totalMembers = 0

                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    if (user != null) {
                        membersList.add(user)
                        totalMembers++
                    }
                }

                // Sort: owner first, then others
                membersList.sortWith(compareBy {
                    if (it.userID == community?.adminID) 0 else 1
                })

                membersCount.text = "$totalMembers members"
                adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to load members", e)
                Toast.makeText(requireContext(), "Failed to load members", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditCommunityNameDialog() {
        val editText = EditText(requireContext()).apply {
            setText(communityName.text)
            setSingleLine()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Community Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != community?.communityName) {
                    updateCommunityName(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
            }
    }

    private fun updateCommunityName(newName: String) {
        FirebaseCommunity.getCommunityReference(communityID!!).update("communityName", newName)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Community name updated", Toast.LENGTH_SHORT).show()
                communityName.text = newName
                community?.communityName = newName
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to update name", e)
                Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCommunityImage(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            FirebaseCommunity.getCommunityReference(communityID!!).update("communityImage", base64)
                .addOnSuccessListener {
                    androidUtils.setProfileImageFromBase64(requireContext(), base64, communityImage)
                    Toast.makeText(requireContext(), "Community photo updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update photo", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("CommunitySettings", "Failed to update image", e)
            Toast.makeText(requireContext(), "Failed to update photo", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(communityID: String): CommunitySettingsFragment {
            return CommunitySettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("communityID", communityID)
                }
            }
        }
    }
}
