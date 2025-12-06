package com.example.Smart_Chat.activities

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.hbb20.CountryCodePicker

class LoginPhoneNumberActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_phone_number)

        val codePicker = findViewById<CountryCodePicker>(R.id.codePicker)
        val inputPhoneNumber = findViewById<EditText>(R.id.inputPhoneNumber)
        val SendOTP = findViewById<Button>(R.id.send_OTP_btn)

        codePicker.registerCarrierNumberEditText(inputPhoneNumber)

        SendOTP.setOnClickListener {
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