package com.codzure.cryptalk.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.DialogPinInputBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PinInputDialogFragment(
    private val onPinEntered: (String) -> Unit,
    private val isForEncryption: Boolean
) : DialogFragment() {

    private var _binding: DialogPinInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPinInputBinding.inflate(LayoutInflater.from(context))

        val title = if (isForEncryption) "🔒 Set PIN for Encryption" else "🔐 Enter PIN to Decrypt"
        val positiveButtonText = if (isForEncryption) "Encrypt" else "Decrypt"

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.DialogSlideAnim)
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(positiveButtonText) { _, _ ->
                val pin = binding.pinEditText.text.toString()
                if (pin.length == 4) onPinEntered(pin)
                else Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}