package com.codzure.cryptalk.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.DialogPinInputBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

enum class PinMode { ENCRYPT, DECRYPT }

class PinInputDialogFragment(
    private val onPinEntered: (String) -> Unit,
    private val mode: PinMode
) : DialogFragment() {

    private var _binding: DialogPinInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPinInputBinding.inflate(LayoutInflater.from(context))

        // Configure dialog based on mode
        val title = when (mode) {
            PinMode.ENCRYPT -> "🔒 Set PIN for Encryption"
            PinMode.DECRYPT -> "🔐 Enter PIN to Decrypt"
        }
        val positiveButtonText = when (mode) {
            PinMode.ENCRYPT -> "Encrypt"
            PinMode.DECRYPT -> "Decrypt"
        }
        val instructionText = when (mode) {
            PinMode.ENCRYPT -> "Enter a 4-digit PIN to secure your message"
            PinMode.DECRYPT -> "Enter your 4-digit PIN to view the message"
        }

        binding.instructionText.text = instructionText
        binding.pinEditText.requestFocus()

        // Real-time PIN validation
        binding.pinEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pin = s.toString()
                binding.pinInputLayout.error = if (pin.length < 4 && pin.isNotEmpty()) {
                    "PIN must be 4 digits"
                } else {
                   "Please enter a valid PIN"
                }
            }
        })

        // Create dialog
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.DialogSlideAnim)
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(positiveButtonText) { _, _ ->
                val pin = binding.pinEditText.text.toString()
                if (isValidPin(pin)) {
                    onPinEntered(pin)
                } else {
                    binding.pinInputLayout.error = "PIN must be 4 digits"
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Auto-show keyboard
        dialog.setOnShowListener {
            binding.pinEditText.post {
                val imm =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.pinEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        return dialog
    }

    private fun isValidPin(pin: String): Boolean = pin.length == 4

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(mode: PinMode, onPinEntered: (String) -> Unit) =
            PinInputDialogFragment(onPinEntered, mode)
    }
}