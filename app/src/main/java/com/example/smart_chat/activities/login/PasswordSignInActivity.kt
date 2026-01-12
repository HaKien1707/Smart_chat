package com.example.smart_chat.activities.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smart_chat.R
import com.example.smart_chat.activities.MainActivity
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.security.PasswordUtils

class PasswordSignInActivity : AppCompatActivity() {

    private lateinit var inputPassword: EditText
    private lateinit var confirmBtn: Button
    private lateinit var progressBar: ProgressBar

    private var phoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_sign_in)

        phoneNumber = intent.getStringExtra("phoneNumber")

        inputPassword = findViewById(R.id.inputPassword)
        confirmBtn = findViewById(R.id.confirm_btn)
        progressBar = findViewById(R.id.progressBar)

        confirmBtn.setOnClickListener {
            val password = inputPassword.text.toString().trim()
            if (password.isEmpty()) {
                inputPassword.error = getString(R.string.enter_password)
                inputPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                return@setOnClickListener
            }

            validatePassword(password)
        }
    }

    private fun validatePassword(password: String) {
        val currentUserId = FirebaseAuthentication.currentUserID()
        if (currentUserId.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.missing_authentication), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setInProgress(true)

        FirebaseAuthentication.currentUserDetails().get()
            .addOnSuccessListener { doc ->
                setInProgress(false)

                if (!doc.exists()) {
                    Toast.makeText(this, getString(R.string.account_not_found), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SignUpActivity::class.java).apply {
                        putExtra("phoneNumber", phoneNumber)
                    }
                    startActivity(intent)
                    finish()
                    return@addOnSuccessListener
                }

                val user = doc.toObject(userModel::class.java)
                val storedHash = user?.password

                if (storedHash.isNullOrBlank()) {
                    Toast.makeText(this, getString(R.string.account_no_password), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val ok = PasswordUtils.verifyPassword(password, storedHash)
                if (!ok) {
                    inputPassword.error = getString(R.string.incorrect_password)
                    inputPassword.setText("")
                    inputPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                    return@addOnSuccessListener
                }

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                setInProgress(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setInProgress(inProgress: Boolean) {
        progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
        confirmBtn.isEnabled = !inProgress
    }
}
