package com.codzure.cryptalk

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class PinInputDialog(
    context: Context,
    private val onPinEntered: (String) -> Unit
) : Dialog(context) {

    private var currentPin = ""
    private lateinit var pinDots: Array<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pin_input_dialog)

        // Configure dialog window
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        // Initialize PIN dots
        pinDots = arrayOf(
            findViewById(R.id.pinDot1),
            findViewById(R.id.pinDot2),
            findViewById(R.id.pinDot3),
            findViewById(R.id.pinDot4)
        )

        // Set dialog description
        findViewById<TextView>(R.id.dialogDescription).text =
            "Enter the PIN to decrypt this QR code"

        // Set up keypad buttons
        setupKeypad()
    }

    private fun setupKeypad() {
        // Number buttons
        for (i in 0..9) {
            findViewById<Button>(context.resources.getIdentifier("btn$i", "id", context.packageName))
                .setOnClickListener { appendDigit(i.toString()) }
        }

        // Clear button
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            if (currentPin.isNotEmpty()) {
                currentPin = currentPin.substring(0, currentPin.length - 1)
                updatePinDots()
            }
        }

        // OK button
        findViewById<Button>(R.id.btnOk).setOnClickListener {
            if (currentPin.length == 4) {
                dismiss()
                onPinEntered(currentPin)
            } else {
                Toast.makeText(context, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun appendDigit(digit: String) {
        if (currentPin.length < 4) {
            currentPin += digit
            updatePinDots()

            // If PIN is complete, automatically submit after a short delay
            if (currentPin.length == 4) {
                Handler(Looper.getMainLooper()).postDelayed({
                    dismiss()
                    onPinEntered(currentPin)
                }, 300)
            }
        }
    }

    private fun updatePinDots() {
        for (i in pinDots.indices) {
            pinDots[i].background = ContextCompat.getDrawable(
                context,
                if (i < currentPin.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
            )
        }
    }
}