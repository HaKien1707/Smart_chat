package com.example.smart_chat.activities.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.hbb20.CountryCodePicker

class LoginPhoneNumberActivity : AppCompatActivity() {

    private lateinit var codePicker: CountryCodePicker
    private lateinit var inputPhoneNumber: EditText
    private lateinit var sendOTPBtn: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_phone_number)

        codePicker = findViewById(R.id.codePicker)
        inputPhoneNumber = findViewById(R.id.inputPhoneNumber)
        sendOTPBtn = findViewById(R.id.send_OTP_btn)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageButton>(R.id.back_btn).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Set default country to Vietnam
        codePicker.setDefaultCountryUsingNameCode("VN")
        codePicker.registerCarrierNumberEditText(inputPhoneNumber)

        sendOTPBtn.setOnClickListener {
            // Validate phone number
            if (!codePicker.isValidFullNumber) {
                inputPhoneNumber.error = "Invalid phone number"
                inputPhoneNumber.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                return@setOnClickListener
            }

            // Get phone number and country code
            val phoneNumber = codePicker.fullNumberWithPlus
            val countryCode = codePicker.selectedCountryNameCode

            // Check if user is registered, then proceed to OTP
            checkUserAndProceed(phoneNumber, countryCode)
        }
    }

    private fun checkUserAndProceed(phoneNumber: String, countryCode: String) {
        val normalizedPhoneNumber = phoneNumber.replace(" ", "")

        setInProgress(true)

        FirebaseAuthentication.allUsersCollection()
            .whereEqualTo("phoneNumber", normalizedPhoneNumber)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                setInProgress(false)
                if (documents.isEmpty) {
                    // Phone number not registered, proceed to OTP for Sign Up
                    proceedToOTP(normalizedPhoneNumber, countryCode, true)
                } else {
                    // User exists, proceed to OTP for Login
                    proceedToOTP(normalizedPhoneNumber, countryCode, false)
                }
            }
            .addOnFailureListener { e ->
                setInProgress(false)
                Log.e("LoginPhone", "Firestore query failed", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun proceedToOTP(phoneNumber: String, countryCode: String, isSignUp: Boolean) {
        val intent = Intent(this, otpActivity::class.java).apply {
            putExtra("phoneNumber", phoneNumber)
            putExtra("countryCode", countryCode)
            putExtra("isSignUp", isSignUp)
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