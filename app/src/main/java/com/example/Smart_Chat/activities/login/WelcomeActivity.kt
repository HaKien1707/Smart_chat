package com.example.Smart_Chat.activities.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager

class WelcomeActivity : AppCompatActivity() {

    private lateinit var startMessagingBtn: Button
    private lateinit var legalTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        startMessagingBtn = findViewById(R.id.start_messaging_btn)
        legalTextView = findViewById(R.id.legal_text)

        setupLegalText()

        startMessagingBtn.setOnClickListener {
            val intent = Intent(this, LoginPhoneNumberActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun setupLegalText() {
        val fullTextWithHtml = getString(R.string.welcome_legal_text)

        // Step 1: Parse the HTML to handle <b> tags and get formatted text.
        val formattedText: Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(fullTextWithHtml, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(fullTextWithHtml)
        }

        // Step 2: Make it mutable to add clickable spans.
        val spannableString = SpannableString(formattedText)

        val termsText = getString(R.string.terms_of_service_title)
        val policyText = getString(R.string.privacy_policy_title)

        // Step 3: Define what happens when links are clicked.
        val termsClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showLegalDialog(termsText, getString(R.string.terms_of_service_content))
            }
        }
        val policyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showLegalDialog(policyText, getString(R.string.privacy_policy_content))
            }
        }

        // Step 4: Find the plain text and apply the clickable spans.
        val plainText = formattedText.toString()
        val termsStart = plainText.indexOf(termsText)
        if (termsStart >= 0) {
            spannableString.setSpan(termsClickableSpan, termsStart, termsStart + termsText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val policyStart = plainText.indexOf(policyText)
        if (policyStart >= 0) {
            spannableString.setSpan(policyClickableSpan, policyStart, policyStart + policyText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Step 5: Set the final text and enable link clicking.
        legalTextView.text = spannableString
        legalTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun showLegalDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}