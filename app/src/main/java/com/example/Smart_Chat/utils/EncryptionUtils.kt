package com.example.Smart_Chat.utils

import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12 // 12 bytes for GCM

    /**
     * Generate a random AES-256 key
     * This will be the shared secret between two users
     */
    fun generateKey(): String {
        try {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(KEY_SIZE)
            val secretKey = keyGenerator.generateKey()

            // Convert to Base64 string for easy storage/transmission
            return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("EncryptionUtils", "Failed to generate key", e)
            throw e
        }
    }

    /**
     * Encrypt a message using AES-GCM
     * Returns: IV + encrypted data (both Base64 encoded)
     */
    fun encrypt(plainText: String, keyString: String): String {
        try {
            // Convert key from Base64 string
            val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
            val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

            // Generate random IV
            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)

            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            // Encrypt
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combine IV + encrypted data
            val combined = iv + encryptedBytes

            // Return as Base64
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("EncryptionUtils", "Encryption failed", e)
            throw e
        }
    }

    /**
     * Decrypt a message using AES-GCM
     * Input: IV + encrypted data (Base64 encoded)
     */
    fun decrypt(cipherText: String, keyString: String): String {
        try {
            // Convert key from Base64 string
            val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
            val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

            // Decode from Base64
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)

            // Extract IV and encrypted data
            val iv = combined.sliceArray(0 until IV_LENGTH)
            val encryptedBytes = combined.sliceArray(IV_LENGTH until combined.size)

            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            // Decrypt
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("EncryptionUtils", "Decryption failed", e)
            throw e
        }
    }

    /**
     * Generate a unique chat ID for private chat
     * Similar to getChatRoomID but for private chats
     */
    fun generatePrivateChatID(userID1: String, userID2: String): String {
        return if (userID1.hashCode() < userID2.hashCode()) {
            "private_${userID1}_${userID2}"
        } else {
            "private_${userID2}_${userID1}"
        }
    }

    /**
     * Verify if a key is valid AES-256 key
     */
    fun isValidKey(keyString: String): Boolean {
        return try {
            val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
            keyBytes.size == KEY_SIZE / 8 // 32 bytes for 256-bit key
        } catch (e: Exception) {
            false
        }
    }
}