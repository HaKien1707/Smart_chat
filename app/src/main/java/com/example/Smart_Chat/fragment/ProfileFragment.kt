package com.example.Smart_Chat.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.activities.splashScreenActivity
import com.example.Smart_Chat.utils.FireBase_utils.currentUserDetails
import com.example.Smart_Chat.utils.FireBase_utils.currentUserID
import com.example.Smart_Chat.utils.FireBase_utils.logout
import com.example.Smart_Chat.utils.androidUtils
import com.example.Smart_Chat.utils.androidUtils.showToast
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.example.Smart_Chat.databinding.FragmentProfileBinding
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private var currentUser: userModel? = null
    private var selectedImageURI: Uri? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    selectedImageURI = uri
                    androidUtils.setProfileImage(requireContext(), uri, binding.profileImage)
                    saveProfileImageToFirestore(uri)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        loadUserData()
        setupClicks()

        return binding.root
    }

    private fun setupClicks() {
        binding.updateBTN.setOnClickListener {
            updateProfile()
        }

        binding.profileImage.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(512)
                .maxResultSize(512, 512)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }
    }

    /**  Load user information from Firestore */
    private fun loadUserData() {
        setInProgress(true)

        currentUserDetails().get()
            .addOnSuccessListener { snapshot ->
                setInProgress(false)

                currentUser = snapshot.toObject(userModel::class.java)
                currentUser?.let { user ->
                    binding.Username.setText(user.username)
                    binding.phoneNumber.setText(user.phoneNumber)

                    if (!user.profileImage.isNullOrEmpty()) {
                        androidUtils.setProfileImageFromBase64(
                            requireContext(),
                            user.profileImage,
                            binding.profileImage
                        )
                    }
                }
            }
            .addOnFailureListener {
                setInProgress(false)
                showToast(requireContext(), "Failed to load user data")
            }
    }

    /** 🔹 Update profile text fields */
    private fun updateProfile() {
        val newUsername = binding.Username.text.toString()

        if (newUsername.length < 3) {
            binding.Username.error = "Invalid username"
            return
        }

        currentUser?.username = newUsername

        setInProgress(true)

        currentUserDetails()
            .set(currentUser!!, SetOptions.merge())
            .addOnSuccessListener {
                setInProgress(false)
                showToast(requireContext(), "Profile updated successfully")
            }
            .addOnFailureListener {
                setInProgress(false)
                showToast(requireContext(), "Failed to update profile")
            }
    }

    /** 🔹 Save selected image to Firestore */
    private fun saveProfileImageToFirestore(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            currentUser?.profileImage = base64

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserID()!!)
                .update("profileImage", base64)
                .addOnSuccessListener {
                    Log.d("PROFILE", "Image updated")
                }
                .addOnFailureListener {
                    Log.e("PROFILE", it.message.toString())
                }
        } catch (e: Exception) {
            Log.e("PROFILE", "Failed to save image", e)
        }
    }

    /** 🔹 UI progress state */
    private fun setInProgress(inProgress: Boolean) {
        binding.updateBTN.apply {
            isEnabled = !inProgress
            visibility = if (inProgress) View.INVISIBLE else View.VISIBLE
        }
    }
}
