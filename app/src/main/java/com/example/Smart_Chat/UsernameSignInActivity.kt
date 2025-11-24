package com.example.Smart_Chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
import com.google.firebase.Timestamp

class UsernameSignInActivity : AppCompatActivity() {

    private lateinit var inputUsername: EditText
    private lateinit var confirmBTN: Button
    private lateinit var profileImage: ImageView

    private var phoneNumber: String? = null
    private var userModel: userModel? = null
    private var isNewUser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username)

        inputUsername = findViewById(R.id.inputUsername)
        confirmBTN = findViewById(R.id.confirmBTN)
        profileImage = findViewById(R.id.icon)

        phoneNumber = intent.getStringExtra("phoneNumber")

        checkIfUserExists()

        // Load user from Firestore
        FireBase_utils.currentUserDetails().get().addOnSuccessListener { doc ->
            val user = doc.toObject(com.example.Smart_Chat.models.userModel::class.java)

            if (user != null && !user.profileImage.isNullOrEmpty()) {
                androidUtils.setProfileImageFromBase64(
                    this,
                    user.profileImage!!,
                    profileImage
                )
            } else {
                profileImage.setImageResource(R.drawable.ic_person)
            }
        }

        confirmBTN.setOnClickListener {
            onConfirmClicked()
        }
    }

    // ------------------------------------------------------------
    // 1. CHECK IF USER EXISTS
    // ------------------------------------------------------------
    private fun checkIfUserExists() {
        FireBase_utils.currentUserDetails().get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("UsernameSignIn", "Failed to check user", task.exception)
                return@addOnCompleteListener
            }

            if (task.result == null) {
                Log.e("UsernameSignIn", "Task result is null")
                return@addOnCompleteListener
            }

            userModel = task.result.toObject(com.example.Smart_Chat.models.userModel::class.java)

            if (userModel != null) {
                // ---- Existing user ----
                isNewUser = false

                inputUsername.setText(userModel?.username)
                inputUsername.isEnabled = false  // cannot change
                confirmBTN.text = getString(R.string.login)
            } else {
                // ---- New user ----
                isNewUser = true

                inputUsername.isEnabled = true
                confirmBTN.text = getString(R.string.signUp)
            }
        }
    }

    // ------------------------------------------------------------
    // 2. BUTTON CLICK
    // ------------------------------------------------------------
    private fun onConfirmClicked() {
        if (!isNewUser) {
            // Existing user: skip and go to Home
            goToMain()
            return
        }

        val username = inputUsername.text.toString().trim()

        if (username.length < 3) {
            inputUsername.error = "Username too short"
            return
        }

        // Check username uniqueness
        checkUsernameAvailable(username)
    }

    // ------------------------------------------------------------
    // 3. CHECK IF USERNAME ALREADY EXISTS
    // ------------------------------------------------------------
    private fun checkUsernameAvailable(username: String) {
        FireBase_utils.allUsersCollection()
            .whereEqualTo("username", username)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Error checking username", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                if (!task.result.isEmpty) {
                    // username exists
                    inputUsername.error = "Username already taken"
                    return@addOnCompleteListener
                }

                // username available → create new user
                createNewUser(username)
            }
    }

    // ------------------------------------------------------------
    // 4. CREATE NEW USER DOCUMENT
    // ------------------------------------------------------------
    private fun createNewUser(username: String) {
        userModel = userModel(
            phoneNumber,
            username,
            Timestamp.now(),
            FireBase_utils.currentUserID(),
            "",
            ""
        )

        FireBase_utils.currentUserDetails()
            .set(userModel!!)
            .addOnSuccessListener {
                goToMain()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
    }

    // ------------------------------------------------------------
    // 5. GO TO MAIN ACTIVITY
    // ------------------------------------------------------------
    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}