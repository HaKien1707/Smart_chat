package com.example.Smart_Chat.activities

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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class otpActivity : AppCompatActivity() {

    private var phoneNumber: String? = null
    private val timeoutSeconds = 30L
    private var verificationId: String? = null
    private var token: PhoneAuthProvider.ForceResendingToken? = null

    private lateinit var inputOTP: EditText
    private lateinit var confirmBTN: Button
    private lateinit var textResendOTP: TextView

    private val mAuth = FirebaseAuth.getInstance()

    private var resendTimer: CountDownTimer? = null
    private val resendIntervalMs = 30_000L // 30 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        phoneNumber = intent.getStringExtra("phoneNumber")

        inputOTP = findViewById(R.id.inputOTP)
        confirmBTN = findViewById(R.id.confirmBTN)
        textResendOTP = findViewById(R.id.resendOTP)

        sendOTP(phoneNumber, false)

        confirmBTN.setOnClickListener {
            val otp = inputOTP.text.toString().trim()
            if (otp.isEmpty()) {
                inputOTP.error = "Enter OTP"
                return@setOnClickListener
            }
            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
            signIn(credential)
        }

        textResendOTP.setOnClickListener {
            // If token is null (rare) we still call sendOTP without force-resend
            sendOTP(phoneNumber, true)
        }
    }

    private fun sendOTP(phoneNumber: String?, isResend: Boolean) {
        startResendTimer() // resets and starts the UI timer

        val builder = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phoneNumber!!)
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                    signIn(phoneAuthCredential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@otpActivity, e.message, Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(
                    s: String,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(s, forceResendingToken)
                    verificationId = s
                    token = forceResendingToken
                    Toast.makeText(this@otpActivity, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                }
            })

        if (isResend && token != null) {
            PhoneAuthProvider.verifyPhoneNumber(builder.setForceResendingToken(token!!).build())
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        }
    }

    private fun signIn(phoneAuthCredential: PhoneAuthCredential) {
        // Disable button while verifying
        confirmBTN.isEnabled = false

        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener { task ->
            confirmBTN.isEnabled = true  // Re-enable button

            if (task.isSuccessful) {
                val intent = Intent(this, UsernameSignInActivity::class.java).apply {
                    putExtra("phoneNumber", phoneNumber)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            } else {
                // Show error on OTP input field
                inputOTP.error = "Invalid OTP"
                inputOTP.setText("")  // Clear the input
                inputOTP.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                val errorMessage = task.exception?.message ?: "Invalid OTP. Please try again."
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startResendTimer() {
        // Cancel previous timer if any
        resendTimer?.cancel()

        textResendOTP.isEnabled = false

        resendTimer = object : CountDownTimer(resendIntervalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                textResendOTP.text = "Resend OTP in $secondsLeft seconds"
            }

            override fun onFinish() {
                textResendOTP.isEnabled = true
                textResendOTP.text = "Resend OTP"
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
        resendTimer = null
    }
}