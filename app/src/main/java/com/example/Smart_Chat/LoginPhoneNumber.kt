package com.example.Smart_Chat

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.hbb20.CountryCodePicker

class LoginPhoneNumber : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_phone_number)

        val codePicker = findViewById<CountryCodePicker>(R.id.codePicker)
        val inputPhoneNumber = findViewById<EditText>(R.id.inputPhoneNumber)
        val sendOTP = findViewById<Button>(R.id.sendOTP)

        codePicker.registerCarrierNumberEditText(inputPhoneNumber)

        sendOTP.setOnClickListener {
            if (!codePicker.isValidFullNumber) {
                inputPhoneNumber.error = "Invalid phone number"
                inputPhoneNumber.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                return@setOnClickListener
            }

            val intent = Intent(this, otpActivity::class.java).apply {
                putExtra("phoneNumber", codePicker.fullNumberWithPlus)
            }
            startActivity(intent)
        }
    }
}