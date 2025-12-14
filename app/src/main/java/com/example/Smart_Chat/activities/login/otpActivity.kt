package com.example.Smart_Chat.activities.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.MainActivity
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class otpActivity : AppCompatActivity() {

    private var phoneNumber: String? = null
    private var countryCode: String? = null
    private var isSignUp: Boolean = false

    // Sign up data
    private var username: String? = null
    private var hashedPassword: String? = null
    private var profileImageBase64: String? = null

    private val timeoutSeconds = 30L
    private var verificationId: String? = null
    private var token: PhoneAuthProvider.ForceResendingToken? = null

    private lateinit var inputOTP: EditText
    private lateinit var confirmOtpBTN: Button
    private lateinit var textResendOTP: TextView

    private val mAuth = FirebaseAuth.getInstance()

    private var resendTimer: CountDownTimer? = null
    private val resendIntervalMs = 30_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        phoneNumber = intent.getStringExtra("phoneNumber")
        countryCode = intent.getStringExtra("countryCode")
        isSignUp = intent.getBooleanExtra("isSignUp", false)

        if (isSignUp) {
            username = intent.getStringExtra("username")
            hashedPassword = intent.getStringExtra("password")
            profileImageBase64 = intent.getStringExtra("profileImage")
        }

        inputOTP = findViewById(R.id.inputOTP)
        confirmOtpBTN = findViewById(R.id.confirm_OTP_btn)
        textResendOTP = findViewById(R.id.resendOTP)

        sendOTP(phoneNumber, false)

        confirmOtpBTN.setOnClickListener {
            val otp = inputOTP.text.toString().trim()

            if (otp.isEmpty()) {
                inputOTP.error = "Enter OTP"
                return@setOnClickListener
            }

            if (verificationId == null) {
                inputOTP.error = "OTP hasn't been sent yet"
                inputOTP.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                Toast.makeText(
                    this,
                    "Please wait, OTP is being sent...",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
            signIn(credential)
        }

        textResendOTP.setOnClickListener {
            sendOTP(phoneNumber, true)
        }
    }

    private fun sendOTP(phoneNumber: String?, isResend: Boolean) {
        if (phoneNumber == null) {
            Toast.makeText(this, "Phone number is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        startResendTimer()

        val builder = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                    signIn(phoneAuthCredential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(
                        this@otpActivity,
                        "Verification failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(
                    s: String,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(s, forceResendingToken)
                    verificationId = s
                    token = forceResendingToken
                    Toast.makeText(
                        this@otpActivity,
                        "OTP sent successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        if (isResend && token != null) {
            PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(token!!).build())
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        }
    }

    private fun signIn(phoneAuthCredential: PhoneAuthCredential) {
        confirmOtpBTN.isEnabled = false

        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener { task ->
            confirmOtpBTN.isEnabled = true

            if (task.isSuccessful) {
                if (isSignUp) {
                    // Create new user account
                    createUserAccount()
                } else {
                    // Existing user - go to username display screen
                    val intent = Intent(this, UsernameSignInActivity::class.java).apply {
                        putExtra("phoneNumber", phoneNumber)
                        putExtra("isLogin", true)
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
            } else {
                inputOTP.error = "Invalid OTP"
                inputOTP.setText("")
                inputOTP.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                val errorMessage = task.exception?.message ?: "Invalid OTP. Please try again."
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createUserAccount() {
        val userId = FireBase_utils.currentUserID()

        val user = userModel(
            userId,
            username,
            phoneNumber,
            hashedPassword,
            null, // email (can be added later)
            countryCode, // nationality - THIS IS CORRECT
            Timestamp.now()
        )

        // Set profile image if provided
        user.profileImage = profileImageBase64

        FireBase_utils.currentUserDetails().set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to create account: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        textResendOTP.isEnabled = false

        resendTimer = object : CountDownTimer(resendIntervalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                textResendOTP.text = getString(R.string.resend_otp_text, secondsLeft)
            }

            override fun onFinish() {
                textResendOTP.isEnabled = true
                textResendOTP.text = getString(R.string.resendOTP)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
        resendTimer = null
    }
}