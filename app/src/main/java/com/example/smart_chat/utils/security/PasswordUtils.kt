package com.example.smart_chat.utils.security

import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {

    /**
     * Hash a password with salt using SHA-256
     * Returns: salt + hash (Base64 encoded)
     */
    fun hashPassword(password: String): String {
        try {
            // Generate random salt
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)

            // Hash password with salt
            val md = MessageDigest.getInstance("SHA-256")
            md.update(salt)
            val hash = md.digest(password.toByteArray())

            // Combine salt + hash
            val combined = salt + hash

            // Return as Base64 (Android's Base64, works on API 24+)
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("PasswordUtils", "Failed to hash password", e)
            throw e
        }
    }

    /**
     * Verify a password against stored hash
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        try {
            // Decode stored hash (Android's Base64)
            val combined = Base64.decode(storedHash, Base64.NO_WRAP)

            // Extract salt (first 16 bytes) and hash (rest)
            val salt = combined.sliceArray(0 until 16)
            val storedPasswordHash = combined.sliceArray(16 until combined.size)

            // Hash the input password with same salt
            val md = MessageDigest.getInstance("SHA-256")
            md.update(salt)
            val inputPasswordHash = md.digest(password.toByteArray())

            // Compare hashes
            return storedPasswordHash.contentEquals(inputPasswordHash)
        } catch (e: Exception) {
            Log.e("PasswordUtils", "Failed to verify password", e)
            return false
        }
    }

    /**
     * Validate password strength
     */
    fun isPasswordValid(password: String): Pair<Boolean, String> {
        return when {
            password.length < 6 -> Pair(false, "Password must be at least 6 characters")
            password.length > 128 -> Pair(false, "Password is too long")
            else -> Pair(true, "")
        }
    }
}