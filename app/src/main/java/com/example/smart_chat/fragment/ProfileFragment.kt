package com.example.smart_chat.fragment

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.smart_chat.R
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.others.androidUtils.showToast
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.firestore.SetOptions
import com.example.smart_chat.databinding.FragmentProfileBinding
import com.example.smart_chat.utils.security.PasswordUtils
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private var currentUser: userModel? = null
    private var selectedImageURI: Uri? = null
    private var newImageBase64: String? = null
    private var isEditMode = false
    private var backPressedCallback: OnBackPressedCallback? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    selectedImageURI = uri
                    androidUtils.setProfileImage(requireContext(), uri, binding.profileImage)
                    newImageBase64 = androidUtils.convertImageToBase64(
                        requireContext(),
                        uri,
                        200,
                        40
                    )
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
        setViewMode(false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (isEditMode) {
                    cancelEditing()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback!!
        )
    }

    private fun setupClicks() {
        binding.editButton.setOnClickListener {
            setViewMode(true)
        }

        binding.updateBTN.setOnClickListener {
            updateProfile()
        }

        binding.cancelButton.setOnClickListener {
            cancelEditing()
        }

        binding.profileImage.setOnClickListener {
            if (isEditMode) {
                ImagePicker.with(this)
                    .cropSquare()
                    .compress(512)
                    .maxResultSize(512, 512)
                    .createIntent { intent -> imagePickerLauncher.launch(intent) }
            }
        }

        // Password change click listener
        binding.password.setOnClickListener {
            if (isEditMode) {
                showChangePasswordDialog()
            }
        }
    }

    private fun setViewMode(editMode: Boolean) {
        isEditMode = editMode

        if (editMode) {
            binding.editButton.visibility = View.GONE
            binding.updateBTN.visibility = View.VISIBLE
            binding.cancelButton.visibility = View.VISIBLE

            binding.Username.isEnabled = true
            binding.email.isEnabled = true

            binding.password.apply {
                isEnabled = editMode          // allow click only in edit mode
                isFocusable = false           // user cannot type
                isFocusableInTouchMode = false
                isClickable = editMode        // allow dialog click
            }


            binding.profileImageHint.visibility = View.VISIBLE

            backPressedCallback?.isEnabled = true
        } else {
            binding.editButton.visibility = View.VISIBLE
            binding.updateBTN.visibility = View.GONE
            binding.cancelButton.visibility = View.GONE

            binding.Username.isEnabled = false
            binding.email.isEnabled = false
            binding.password.isEnabled = false

            binding.profileImageHint.visibility = View.GONE

            backPressedCallback?.isEnabled = false
        }

        binding.phoneNumber.isEnabled = false
        binding.nationality.isEnabled = false
    }

    private fun loadUserData() {
        setInProgress(true)

        FirebaseAuthentication.currentUserDetails().get()
            .addOnSuccessListener { snapshot ->
                setInProgress(false)

                currentUser = snapshot.toObject(userModel::class.java)
                currentUser?.let { user ->
                    binding.Username.setText(user.username)
                    binding.phoneNumber.setText(user.phoneNumber)
                    binding.email.setText(user.email ?: "")

                    val countryCode = user.nationality
                    if (!countryCode.isNullOrEmpty()) {
                        val locale = Locale("", countryCode)
                        binding.nationality.setText(locale.displayCountry)
                    } else {
                        binding.nationality.setText("Not set")
                    }

                    val passwordDisplay = if (user.password != null) "********" else "Not set"
                    binding.password.setText(passwordDisplay)

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

    private fun updateProfile() {
        val newUsername = binding.Username.text.toString().trim()
        val newEmail = binding.email.text.toString().trim()

        if (newUsername.isEmpty()) {
            binding.Username.error = "Username cannot be empty"
            return
        }

        if (newUsername.length < 3) {
            binding.Username.error = "Username must be at least 3 characters"
            return
        }

        if (newEmail.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            binding.email.error = "Invalid email format"
            return
        }

        currentUser?.username = newUsername
        currentUser?.email = newEmail.ifEmpty { null }

        if (newImageBase64 != null) {
            currentUser?.profileImage = newImageBase64
        }

        setInProgress(true)

        FirebaseAuthentication.currentUserDetails()
            .set(currentUser!!, SetOptions.merge())
            .addOnSuccessListener {
                setInProgress(false)
                showToast(requireContext(), "Profile updated successfully")

                selectedImageURI = null
                newImageBase64 = null

                setViewMode(false)
                loadUserData()
            }
            .addOnFailureListener {
                setInProgress(false)
                showToast(requireContext(), "Failed to update profile")
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val oldPasswordInput = dialogView.findViewById<EditText>(R.id.old_password)
        val newPasswordInput = dialogView.findViewById<EditText>(R.id.new_password)
        val confirmPasswordInput = dialogView.findViewById<EditText>(R.id.confirm_password)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val oldPassword = oldPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                changePassword(oldPassword, newPassword, confirmPassword)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        if (currentUser?.password == null || !PasswordUtils.verifyPassword(oldPassword, currentUser?.password!!)) {
            showToast(requireContext(), "Incorrect old password")
            return
        }

        val (isValid, errorMsg) = PasswordUtils.isPasswordValid(newPassword)
        if (!isValid) {
            showToast(requireContext(), errorMsg)
            return
        }

        if (newPassword != confirmPassword) {
            showToast(requireContext(), "Passwords don't match")
            return
        }

        val hashedPassword = PasswordUtils.hashPassword(newPassword)
        FirebaseAuthentication.currentUserDetails().update("password", hashedPassword)
            .addOnSuccessListener {
                showToast(requireContext(), "Password changed successfully")
                currentUser?.password = hashedPassword
            }
            .addOnFailureListener {
                showToast(requireContext(), "Failed to change password")
            }
    }

    private fun cancelEditing() {
        selectedImageURI = null
        newImageBase64 = null
        loadUserData()
        setViewMode(false)
    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.updateBTN.isEnabled = false
            binding.cancelButton.isEnabled = false
            binding.editButton.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.updateBTN.isEnabled = true
            binding.cancelButton.isEnabled = true
            binding.editButton.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPressedCallback?.remove()
    }
}