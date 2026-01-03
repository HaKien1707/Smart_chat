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
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication
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

    private val timeoutSeconds = 60L
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private lateinit var inputOTP: EditText
    private lateinit var confirmOtpBTN: Button
    private lateinit var textResendOTP: TextView

    private val mAuth = FirebaseAuth.getInstance()

    private var resendTimer: CountDownTimer? = null
    private val resendIntervalMs = 60_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        phoneNumber = intent.getStringExtra("phoneNumber")
        countryCode = intent.getStringExtra("countryCode")
        isSignUp = intent.getBooleanExtra("isSignUp", false)

        inputOTP = findViewById(R.id.inputOTP)
        confirmOtpBTN = findViewById(R.id.confirm_OTP_btn)
        textResendOTP = findViewById(R.id.resendOTP)

        sendOTP(phoneNumber, false)

        confirmOtpBTN.setOnClickListener {
            val otp = inputOTP.text.toString().trim()

            if (otp.isEmpty() || otp.length < 6) {
                inputOTP.error = "Enter valid OTP"
                inputOTP.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                return@setOnClickListener
            }

            if (verificationId == null) {
                Toast.makeText(this, "Please wait for the OTP to be sent.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
            signInWithCredential(credential)
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

        val optionsBuilder = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@otpActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@otpActivity.verificationId = verificationId
                    this@otpActivity.resendToken = token
                    Toast.makeText(this@otpActivity, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                }
            })

        if (isResend && resendToken != null) {
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.setForceResendingToken(resendToken!!).build())
        } else {
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        confirmOtpBTN.isEnabled = false

        mAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            confirmOtpBTN.isEnabled = true

            if (task.isSuccessful) {
                if (isSignUp) {
                    // OTP verified for a new user, now go to SignUpActivity to create the account
                    val intent = Intent(this, SignUpActivity::class.java).apply {
                        putExtra("phoneNumber", phoneNumber)
                        putExtra("countryCode", countryCode)
                        // We carry the verified credential's info implicitly by having the user signed in
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Existing user logged in, go to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
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
    }
}