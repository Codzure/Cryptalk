package com.codzure.cryptalk.utility

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject
import java.util.*

/**
 * Utility class for QR code generation and processing in Cryptalk
 */
object QRCodeManager {

    /**
     * Generates a QR code containing encrypted data
     *
     * @param data The data to encode in the QR code
     * @param pin The PIN to encrypt the data with
     * @param width Width of the QR code bitmap
     * @param height Height of the QR code bitmap
     * @return A Bitmap containing the QR code
     */
    fun generateEncryptedQRCode(data: String, pin: String, width: Int, height: Int): Bitmap {
        // Create a JSON object with all the necessary metadata
        val qrData = JSONObject().apply {
            // Add app identifier to ensure only Cryptalk can process this QR code
            put("app", "cryptalk")
            put("version", 1)
            put("timestamp", System.currentTimeMillis())

            // Encrypt the actual data
            val encryptedData = CryptalkEncryption.encryptMessage(data, pin)
            put("data", encryptedData)

            // Store hash of PIN for validation when scanning
            val pinHash = CryptalkEncryption.hashPin(pin)
            put("pin_hash", pinHash)
        }

        // Generate QR code with the JSON data
        return generateQRCode(qrData.toString(), width, height)
    }

    /**
     * Decodes and decrypts data from a QR code
     *
     * @param qrContent The string content decoded from a QR code
     * @param pin The PIN to decrypt the data with
     * @return The decrypted data
     * @throws Exception if the QR code is invalid or PIN is incorrect
     */
    fun decryptQRCodeContent(qrContent: String, pin: String): String {
        try {
            // Parse JSON content
            val json = JSONObject(qrContent)

            // Validate that this is a Cryptalk QR code
            if (json.optString("app") != "cryptalk") {
                throw Exception("Invalid QR code: not a Cryptalk code")
            }

            // Validate PIN hash
            val storedPinHash = json.getString("pin_hash")
            if (!CryptalkEncryption.validatePin(pin, storedPinHash)) {
                throw Exception("Incorrect PIN")
            }

            // Decrypt the data
            val encryptedData = json.getString("data")
            return CryptalkEncryption.decryptMessage(encryptedData, pin)
        } catch (e: Exception) {
            throw Exception("Failed to decrypt QR code: ${e.message}")
        }
    }

    /**
     * Creates a sharing intent with QR data
     *
     * @param title Title for sharing (e.g., "My Wallet Address")
     * @param data The data to encrypt and share
     * @param pin The PIN to encrypt the data with
     * @return A JSON string containing the shareable QR data
     */
    fun createShareableQRData(title: String, data: String, pin: String): String {
        val qrData = JSONObject().apply {
            put("app", "cryptalk")
            put("version", 1)
            put("title", title)
            put("timestamp", System.currentTimeMillis())
            put("data", CryptalkEncryption.encryptMessage(data, pin))
            put("pin_hash", CryptalkEncryption.hashPin(pin))
        }

        return qrData.toString()
    }

    /**
     * Generates a QR code bitmap from string content
     */
    private fun generateQRCode(content: String, width: Int, height: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H) // Highest error correction
            put(EncodeHintType.MARGIN, 2) // Quieter zone around QR
        }

        try {
            val bitMatrix = MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, width, height, hints
            )
            return createBitmap(bitMatrix)
        } catch (e: WriterException) {
            throw RuntimeException("Failed to generate QR code: ${e.message}")
        }
    }

    /**
     * Creates a bitmap from a bit matrix
     */
    private fun createBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}

////***
// * Example activity for generating and sharing QR codes
// *//*
//class QRCodeGeneratorActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityQrCodeGeneratorBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityQrCodeGeneratorBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.generateButton.setOnClickListener {
//            val secretData = binding.secretDataInput.text.toString().trim()
//            val pin = binding.pinInput.text.toString().trim()
//            val title = binding.titleInput.text.toString().trim()
//
//            if (secretData.isEmpty() || pin.isEmpty() || pin.length != 4) {
//                Toast.makeText(this, "Please enter valid data and a 4-digit PIN", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            try {
//                // Generate QR code bitmap
//                val qrBitmap = QRCodeManager.generateEncryptedQRCode(
//                    secretData, pin, 512, 512
//                )
//
//                // Display QR code
//                binding.qrCodeImage.setImageBitmap(qrBitmap)
//                binding.qrCodeContainer.visibility = View.VISIBLE
//
//                // Enable share button
//                binding.shareButton.isEnabled = true
//                binding.shareButton.setOnClickListener {
//                    shareQRCode(title, secretData, pin)
//                }
//
//            } catch (e: Exception) {
//                Toast.makeText(this, "Error generating QR code: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun shareQRCode(title: String, data: String, pin: String) {
//        val shareableData = QRCodeManager.createShareableQRData(title, data, pin)
//
//        // Create share intent
//        val shareIntent = Intent().apply {
//            action = Intent.ACTION_SEND
//            putExtra(Intent.EXTRA_TEXT, "I'm sharing a secure Cryptalk QR code with you.\n\n$shareableData")
//            type = "text/plain"
//        }
//
//        startActivity(Intent.createChooser(shareIntent, "Share Encrypted Data"))
//    }
//}
//
//*//**
// * Example activity for scanning and decrypting QR codes
// *//*
//class QRCodeScannerActivity : AppCompatActivity() {
//
//    private val REQUEST_CAMERA_PERMISSION = 100
//    private val REQUEST_QR_SCAN = 101
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_qr_scanner)
//
//        findViewById<Button>(R.id.scanButton).setOnClickListener {
//            // Check for camera permission
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_GRANTED) {
//                startQRScanner()
//            } else {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.CAMERA),
//                    REQUEST_CAMERA_PERMISSION
//                )
//            }
//        }
//    }
//
//    private fun startQRScanner() {
//        // Launch camera/QR code scanner - implementation depends on your chosen scanner library
//        // For example, using ZXing's IntentIntegrator:
//        val intentIntegrator = IntentIntegrator(this)
//        intentIntegrator.setBeepEnabled(false)
//        intentIntegrator.setOrientationLocked(false)
//        intentIntegrator.setPrompt("Scan a Cryptalk QR Code")
//        intentIntegrator.initiateScan()
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        // Handle QR scan result
//        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
//        if (result != null && result.contents != null) {
//            processQRContent(result.contents)
//        }
//    }
//
//    private fun processQRContent(qrContent: String) {
//        try {
//            // Check if it's a Cryptalk QR code
//            val json = JSONObject(qrContent)
//            if (json.optString("app") != "cryptalk") {
//                Toast.makeText(this, "Not a valid Cryptalk QR code", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            // Show PIN input dialog
//            showPinInputDialog(qrContent)
//
//        } catch (e: Exception) {
//            Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun showPinInputDialog(qrContent: String) {
//        val dialog = PinInputDialog(this) { pin ->
//            try {
//                // Attempt to decrypt with provided PIN
//                val decryptedData = QRCodeManager.decryptQRCodeContent(qrContent, pin)
//
//                // Show decrypted data
//                showDecryptedDataDialog(decryptedData)
//
//            } catch (e: Exception) {
//                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        dialog.show()
//    }
//
//    private fun showDecryptedDataDialog(decryptedData: String) {
//        AlertDialog.Builder(this)
//            .setTitle("Decrypted Data")
//            .setMessage(decryptedData)
//            .setPositiveButton("Copy") { _, _ ->
//                // Copy to clipboard
//                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                val clip = ClipData.newPlainText("Cryptalk Data", decryptedData)
//                clipboard.setPrimaryClip(clip)
//                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
//            }
//            .setNegativeButton("Close", null)
//            .show()
//    }
//}
//*/
/**
 * Custom PIN input dialog
 */