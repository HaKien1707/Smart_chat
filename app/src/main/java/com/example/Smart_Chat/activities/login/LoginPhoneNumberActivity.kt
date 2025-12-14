package com.example.Smart_Chat.activities.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.PasswordUtils
import com.example.Smart_Chat.utils.ThemeManager
import com.hbb20.CountryCodePicker

class LoginPhoneNumberActivity : AppCompatActivity() {

    private lateinit var codePicker: CountryCodePicker
    private lateinit var inputPhoneNumber: EditText
    private lateinit var inputPassword: EditText
    private lateinit var sendOTPBtn: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_phone_number)

        codePicker = findViewById(R.id.codePicker)
        inputPhoneNumber = findViewById(R.id.inputPhoneNumber)
        inputPassword = findViewById(R.id.inputPassword)
        sendOTPBtn = findViewById(R.id.send_OTP_btn)
        progressBar = findViewById(R.id.progressBar)

        codePicker.registerCarrierNumberEditText(inputPhoneNumber)

        sendOTPBtn.setOnClickListener {
            // Validate phone number
            if (!codePicker.isValidFullNumber) {
                inputPhoneNumber.error = "Invalid phone number"
                inputPhoneNumber.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                return@setOnClickListener
            }

            // Validate password
            val password = inputPassword.text.toString().trim()
            if (password.isEmpty()) {
                inputPassword.error = "Enter password"
                inputPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                return@setOnClickListener
            }

            // Get phone number and country code
            val phoneNumber = codePicker.fullNumberWithPlus
            val countryCode = codePicker.selectedCountryNameCode

            // Verify password before sending OTP
            verifyPasswordAndSendOTP(phoneNumber, password, countryCode)
        }
    }

    private fun verifyPasswordAndSendOTP(phoneNumber: String, password: String, countryCode: String) {
        setInProgress(true)

        FireBase_utils.allUsersCollection()
            .whereEqualTo("phoneNumber", phoneNumber)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    setInProgress(false)
                    Toast.makeText(
                        this,
                        "Phone number not registered. Please sign up first.",
                        Toast.LENGTH_LONG
                    ).show()
                    inputPhoneNumber.error = "Not registered"
                    inputPhoneNumber.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                } else {
                    val user = documents.documents[0].toObject(userModel::class.java)
                    val storedPasswordHash = user?.password

                    Log.d("LoginPhone", "Phone: $phoneNumber")
                    Log.d("LoginPhone", "Stored hash exists: ${storedPasswordHash != null}")
                    Log.d("LoginPhone", "Entered password: $password")

                    if (storedPasswordHash == null) {
                        // OLD USER WITHOUT PASSWORD - Set default password "000000"
                        Log.d("LoginPhone", "Old user detected, setting default password")

                        val defaultPassword = PasswordUtils.hashPassword("000000")

                        val updates = hashMapOf<String, Any>(
                            "password" to defaultPassword,
                            "nationality" to countryCode
                        )

                        documents.documents[0].reference.update(updates)
                            .addOnSuccessListener {
                                setInProgress(false)  // ✅ Move here
                                Log.d("LoginPhone", "Default password and nationality set successfully")

                                if (password == "000000") {
                                    proceedToOTP(phoneNumber, countryCode)
                                } else {
                                    inputPassword.error = "Use default password: 000000"
                                    Toast.makeText(
                                        this,
                                        "Your default password is: 000000\nChange it in settings after login.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                setInProgress(false)  // ✅ Move here
                                Log.e("LoginPhone", "Failed to set default password", e)
                                Toast.makeText(this, "Failed to set default password: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        // User has password - verify it
                        setInProgress(false)  // ✅ Add here too
                        Log.d("LoginPhone", "Verifying password...")

                        val isPasswordCorrect = PasswordUtils.verifyPassword(password, storedPasswordHash)
                        Log.d("LoginPhone", "Password verification result: $isPasswordCorrect")

                        if (isPasswordCorrect) {
                            Log.d("LoginPhone", "Password correct, proceeding to OTP")
                            proceedToOTP(phoneNumber, countryCode)
                        } else {
                            Log.d("LoginPhone", "Password incorrect")
                            inputPassword.error = "Incorrect password"
                            inputPassword.setText("")
                            inputPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                setInProgress(false)
                Log.e("LoginPhone", "Firestore query failed", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun proceedToOTP(phoneNumber: String, countryCode: String) {
        val intent = Intent(this, otpActivity::class.java).apply {
            putExtra("phoneNumber", phoneNumber)
            putExtra("countryCode", countryCode)
            putExtra("isSignUp", false)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            progressBar.visibility = View.VISIBLE
            sendOTPBtn.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            sendOTPBtn.isEnabled = true
        }
    }
}