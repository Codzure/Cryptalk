package com.codzure.cryptalk.utility

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class for encryption operations in Cryptalk
 */
object CryptalkEncryption {
    private const val AES_ALGORITHM = "AES/CBC/PKCS7Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION_COUNT = 10000

    /**
     * Encrypts a message using AES encryption with a PIN
     *
     * @param message The plaintext message to encrypt
     * @param pin The 4-digit PIN to use for encryption
     * @return A Base64-encoded string containing the IV and encrypted data
     */
    fun encryptMessage(message: String, pin: String): String {
        // Generate a random salt for key derivation
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        // Generate a random IV (Initialization Vector)
        val iv = ByteArray(16)
        random.nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        // Convert PIN to encryption key using PBKDF2
        val secretKey = generateSecretKey(pin, salt)

        // Initialize cipher for encryption
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        // Encrypt the message
        val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))

        // Combine salt + IV + encrypted bytes for storage
        val combined = ByteArray(salt.size + iv.size + encryptedBytes.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, salt.size + iv.size, encryptedBytes.size)

        // Encode as Base64 string for storage
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts a message using AES encryption with a PIN
     *
     * @param encryptedData The Base64-encoded encrypted data
     * @param pin The 4-digit PIN to use for decryption
     * @return The decrypted plaintext message
     * @throws Exception if decryption fails (wrong PIN or corrupted data)
     */
    fun decryptMessage(encryptedData: String, pin: String): String {
        // Decode the Base64 string
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)

        // Extract salt, IV and encrypted data
        val salt = ByteArray(16)
        val iv = ByteArray(16)
        val encryptedBytes = ByteArray(combined.size - salt.size - iv.size)

        System.arraycopy(combined, 0, salt, 0, salt.size)
        System.arraycopy(combined, salt.size, iv, 0, iv.size)
        System.arraycopy(combined, salt.size + iv.size, encryptedBytes, 0, encryptedBytes.size)

        // Derive the secret key from PIN and salt
        val secretKey = generateSecretKey(pin, salt)

        // Initialize cipher for decryption
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

        // Decrypt the message
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Generates a SHA-256 hash of the PIN for storage/comparison
     *
     * @param pin The PIN to hash
     * @return Hex string of the hashed PIN
     */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validates if a PIN hash matches a given PIN
     *
     * @param pin The PIN to validate
     * @param storedHash The stored hash to compare against
     * @return True if the PIN matches the stored hash
     */
    fun validatePin(pin: String, storedHash: String): Boolean {
        return hashPin(pin) == storedHash
    }

    /**
     * Generates a secret key from PIN and salt using PBKDF2
     */
    private fun generateSecretKey(pin: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Checks if data is likely encrypted (looks like valid Base64)
     */
    fun isLikelyEncrypted(data: String): Boolean {
        return try {
            val decoded = Base64.decode(data, Base64.NO_WRAP)
            decoded.size > 32 // At minimum we need salt + IV (32 bytes)
        } catch (e: Exception) {
            false
        }
    }
}