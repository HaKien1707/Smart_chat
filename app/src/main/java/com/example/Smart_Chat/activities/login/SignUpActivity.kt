package com.example.Smart_Chat.activities.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.security.PasswordUtils
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.others.androidUtils
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication
import com.github.dhaval2404.imagepicker.ImagePicker

class SignUpActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var inputUsername: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputConfirmPassword: EditText
    private lateinit var nextBtn: Button
    private lateinit var progressBar: ProgressBar

    private var phoneNumber: String? = null
    private var countryCode: String? = null
    private var selectedImageUri: Uri? = null
    private var profileImageBase64: String? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    selectedImageUri = uri
                    androidUtils.setProfileImage(this, uri, profileImage)

                    // Convert to base64
                    profileImageBase64 = androidUtils.convertImageToBase64(this, uri, 200, 40)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Get phone number from previous activity
        phoneNumber = intent.getStringExtra("phoneNumber")
        countryCode = intent.getStringExtra("countryCode")

        profileImage = findViewById(R.id.profile_image)
        inputUsername = findViewById(R.id.inputUsername)
        inputPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)
        nextBtn = findViewById(R.id.next_btn)
        progressBar = findViewById(R.id.progressBar)

        profileImage.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(512)
                .maxResultSize(512, 512)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }

        nextBtn.setOnClickListener {
            validateAndProceed()
        }
    }

    private fun validateAndProceed() {
        // Validate username
        val username = inputUsername.text.toString().trim()
        if (username.isEmpty()) {
            inputUsername.error = "Username required"
            inputUsername.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
            return
        }

        if (username.length < 3) {
            inputUsername.error = "Username must be at least 3 characters"
            inputUsername.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
            return
        }

        // Validate password
        val password = inputPassword.text.toString().trim()
        if (password.isEmpty()) {
            inputPassword.error = "Password required"
            inputPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
            return
        }

        val (isValid, errorMsg) = PasswordUtils.isPasswordValid(password)
        if (!isValid) {
            inputPassword.error = errorMsg
            inputPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
            return
        }

        // Validate confirm password
        val confirmPassword = inputConfirmPassword.text.toString().trim()
        if (password != confirmPassword) {
            inputConfirmPassword.error = "Passwords don't match"
            inputConfirmPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
            return
        }

        // All fields are valid, check username availability
        checkUsernameAvailability()
    }

    private fun checkUsernameAvailability() {
        setInProgress(true)
        val username = inputUsername.text.toString().trim()

        FirebaseAuthentication.allUsersCollection()
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                setInProgress(false)

                if (!documents.isEmpty) {
                    // Username already taken
                    inputUsername.error = "Username already taken"
                    Toast.makeText(
                        this,
                        "Username already taken. Please choose another.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Everything is valid, proceed to OTP
                    proceedToOTP()
                }
            }
            .addOnFailureListener { e ->
                setInProgress(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun proceedToOTP() {
        val username = inputUsername.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        // Hash password
        val hashedPassword = try {
            PasswordUtils.hashPassword(password)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to secure password", Toast.LENGTH_SHORT).show()
            return
        }

        // Go to OTP with all data
        val intent = Intent(this, otpActivity::class.java).apply {
            putExtra("phoneNumber", phoneNumber)
            putExtra("countryCode", countryCode)
            putExtra("username", username)
            putExtra("password", hashedPassword)
            putExtra("profileImage", profileImageBase64)
            putExtra("isSignUp", true) // Flag to indicate sign up flow
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            progressBar.visibility = View.VISIBLE
            nextBtn.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            nextBtn.isEnabled = true
        }
    }
}