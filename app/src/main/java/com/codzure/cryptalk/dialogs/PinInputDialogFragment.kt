package com.codzure.cryptalk.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.DialogPinInputBinding
import com.codzure.cryptalk.extensions.hideKeyboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder

enum class PinMode { ENCRYPT, DECRYPT }

class PinInputDialogFragment(
    private val onPinEntered: (String) -> Unit,
    private val mode: PinMode
) : DialogFragment() {

    private var _binding: DialogPinInputBinding? = null
    private val binding get() = _binding!!

    private val pinBuilder = StringBuilder()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPinInputBinding.inflate(LayoutInflater.from(context))

        // Configure UI texts based on mode
        val title = when (mode) {
            PinMode.ENCRYPT -> "🔒 Set PIN for Encryption"
            PinMode.DECRYPT -> "🔐 Enter PIN to Decrypt"
        }
        val instructionText = when (mode) {
            PinMode.ENCRYPT -> "This PIN will be used to encrypt your message."
            PinMode.DECRYPT -> "Enter your 4-digit PIN to view the message."
        }

        val positiveButtonText = when (mode) {
            PinMode.ENCRYPT -> "Encrypt"
            PinMode.DECRYPT -> "Decrypt"
        }

        binding.dialogTitle.text = title
        binding.dialogDescription.text = instructionText
        binding.btnAction.text = positiveButtonText

        // Set up numeric keypad button listeners
        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        buttons.forEach { button ->
            button.setOnClickListener {
                if (pinBuilder.length < 4) {
                    pinBuilder.append(button.text)
                    updatePinDots()
                }
            }
        }

        binding.btnClear.setOnClickListener {
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                updatePinDots()
            }
        }

        binding.btnOk.setOnClickListener {
            val pin = pinBuilder.toString()
            if (isValidPin(pin)) {
                onPinEntered(pin)
                dismiss()
                hideKeyboard()
            } else {
                Toast.makeText(requireContext(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAction.setOnClickListener {
            val pin = pinBuilder.toString()
            if (isValidPin(pin)) {
                onPinEntered(pin)
                dismiss()
                hideKeyboard()
            } else {
                Toast.makeText(requireContext(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
            hideKeyboard()
        }

        return Dialog(requireContext(), R.style.DialogSlideAnim).apply {
            setContentView(binding.root)
        }
    }

    private fun isValidPin(pin: String): Boolean = pin.length == 4

    private fun updatePinDots() {
        val dots = listOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4)
        for (i in dots.indices) {
            val drawableRes = if (i < pinBuilder.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
            dots[i].setBackgroundResource(drawableRes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(mode: PinMode, onPinEntered: (String) -> Unit) =
            PinInputDialogFragment(onPinEntered, mode)
    }
}