package com.codzure.cryptalk.extensions

import android.annotation.SuppressLint
import javax.crypto.Cipher
import android.util.Base64
import javax.crypto.spec.SecretKeySpec

object AESAlgorithm {

    object AES {
        private const val ALGORITHM = "AES"

        @SuppressLint("GetInstance")
        fun encrypt(input: String, pin: String): String {
            val secretKey = SecretKeySpec(padPin(pin), ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(input.toByteArray())
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }

        @SuppressLint("GetInstance")
        fun decrypt(encrypted: String, pin: String): String {
            val secretKey = SecretKeySpec(padPin(pin), ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decryptedBytes = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
            return String(decryptedBytes)
        }

        // Pad 4-digit PIN to 16-byte key
        private fun padPin(pin: String): ByteArray {
            return pin.padEnd(16, '0').toByteArray()
        }
    }
}