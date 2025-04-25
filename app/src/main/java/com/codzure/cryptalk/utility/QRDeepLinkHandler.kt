package com.codzure.cryptalk.utility

import android.content.Intent
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Helper class to handle QR sharing via deep links
 */
object QRDeepLinkHandler {

    private const val SCHEME = "cryptalk://"
    private const val HOST_QR = "qr"

    /**
     * Creates a deep link for a QR code
     *
     * @param title The title of the shared data
     * @param data The data to encrypt
     * @param pin The PIN to encrypt with
     * @return A deep link URI string
     */
    fun createQRDeepLink(title: String, data: String, pin: String): String {
        val qrData = QRCodeManager.createShareableQRData(title, data, pin)
        val encodedData = URLEncoder.encode(qrData, "UTF-8")
        return "$SCHEME$HOST_QR?data=$encodedData"
    }

    /**
     * Processes a deep link from an intent
     *
     * @param intent The intent potentially containing a deep link
     * @return The QR data if valid, null otherwise
     */
    fun processDeepLink(intent: Intent): String? {
        val uri = intent.data ?: return null

        if (uri.scheme == SCHEME.removeSuffix("://") && uri.host == HOST_QR) {
            return uri.getQueryParameter("data")?.let {
                URLDecoder.decode(it, "UTF-8")
            }
        }

        return null
    }
}


//class ChatActivity : AppCompatActivity() {
//
//    // ... other activity code ...
//
//    private fun setupQRCodeButton() {
//        binding.qrCodeButton.setOnClickListener {
//            showQROptionsDialog()
//        }
//    }
//
//    private fun showQROptionsDialog() {
//        val options = arrayOf("Generate QR Code", "Scan QR Code")
//
//        AlertDialog.Builder(this)
//            .setTitle("QR Code Options")
//            .setItems(options) { _, which ->
//                when (which) {
//                    0 -> startActivity(Intent(this, QRCodeGeneratorActivity::class.java))
//                    1 -> startActivity(Intent(this, QRCodeScannerActivity::class.java))
//                }
//            }
//            .show()
//    }
//
//    /**
//     * Handle shared QR codes or deep links
//     */
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//
//        // Check if this is a QR deep link
//        QRDeepLinkHandler.processDeepLink(intent)?.let { qrData ->
//            try {
//                val json = JSONObject(qrData)
//                val title = json.optString("title", "Encrypted Data")
//
//                // Show PIN input dialog to decrypt
//                PinInputDialog(this) { pin ->
//                    try {
//                        val decryptedData = QRCodeManager.decryptQRCodeContent(qrData, pin)
//                        showDecryptedContent(title, decryptedData)
//                    } catch (e: Exception) {
//                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//                    }
//                }.show()
//            } catch (e: Exception) {
//                Toast.makeText(this, "Invalid QR data format", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun showDecryptedContent(title: String, content: String) {
//        val dialog = AlertDialog.Builder(this)
//            .setTitle(title)
//            .setMessage(content)
//            .setPositiveButton("Copy") { _, _ ->
//                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                val clip = ClipData.newPlainText(title, content)
//                clipboard.setPrimaryClip(clip)
//                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
//            }
//            .setNegativeButton("Close", null)
//            .create()
//
//        dialog.show()
//    }
//}