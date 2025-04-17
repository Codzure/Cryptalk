package com.codzure.cryptalk.home

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.FragmentChatBinding
import com.codzure.cryptalk.extensions.AESAlgorithm.AES
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupInputField()
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(messages) { message ->
            if (message.pinHash != null) {
                showPinDialog { inputPin ->
                    if (hashPin(inputPin) == message.pinHash) {
                        try {
                            val decrypted = AES.decrypt(message.encodedText, inputPin)
                            showDecryptedDialog(decrypted)
                        } catch (e: Exception) {
                            showErrorSnackbar("Decryption failed. Please try again.")
                        }
                    } else {
                        showErrorSnackbar("Incorrect PIN. Please try again.")
                    }
                }
            } else {
                // If the message is not encrypted, just show it directly
                //showDecryptedDialog(message.encodedText)
            }
        }

        binding.messageList.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = this@ChatFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupInputField() {
        binding.apply {
            messageInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    promptForEncryption()
                    true
                } else false
            }

            zapSendButton.setOnClickListener {
                promptForEncryption()
            }

            messageInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    zapSendButton.isEnabled = s?.isNotEmpty() == true
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun promptForEncryption() {
        val messageText = binding.messageInput.text.toString().trim()
        if (messageText.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle("Encrypt this message?")
            .setMessage("Would you like to encrypt this message with a 4-digit PIN?")
            .setPositiveButton("Encrypt") { _, _ ->
                askForPin { pin ->
                    sendMessage(messageText, pin)
                }
            }
            .setNegativeButton("Send Plain") { _, _ ->
                sendMessage(messageText, null)
            }
            .show()
    }

    private fun askForPin(onPinEntered: (String) -> Unit) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "4-digit PIN"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Enter PIN")
            .setView(input)
            .setPositiveButton("Encrypt") { _, _ ->
                val pin = input.text.toString()
                if (pin.length == 4) onPinEntered(pin)
                else showErrorSnackbar("PIN must be 4 digits")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendMessage(messageText: String, pin: String?) {
        try {
            val (finalMessage, pinHash) = if (pin != null) {
                AES.encrypt(messageText, pin) to hashPin(pin)
            } else {
                messageText to null
            }

            val newMessage = Message(
                id = System.currentTimeMillis().toString(),
                sender = "me", // or however you're identifying the sender
                encodedText = finalMessage,
                pinHash = pinHash,
                isEncrypted = pin != null,
                timestamp = System.currentTimeMillis()
            )

            messages.add(newMessage)
            adapter.notifyItemInserted(messages.size - 1)
            binding.messageList.smoothScrollToPosition(messages.size - 1)

            binding.messageInput.text?.clear()

        } catch (e: Exception) {
            showErrorSnackbar("Encryption failed: ${e.message}")
        }
    }

    private fun showDecryptedDialog(text: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("✨ Secret Message")
            .setMessage(text)
            .setPositiveButton("Got it!", null)
            .show()
    }

    private fun showPinDialog(onPinEntered: (String) -> Unit) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter 4-digit PIN"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("🔒 Encrypted Message")
            .setView(input)
            .setPositiveButton("Decrypt") { _, _ ->
                val pin = input.text.toString()
                if (pin.length == 4) onPinEntered(pin)
                else showErrorSnackbar("PIN must be 4 digits")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.error_red, null))
            .setTextColor(resources.getColor(R.color.text_light, null))
            .show()
    }

    private fun hashPin(pin: String): String {
        return (pin.hashCode() xor 0x5f3759df).toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


/*val adapter = MessageAdapter(messages) { message ->
    if (message.pinHash != null) {
        showPinDialog { inputPin ->
            if (hashPin(inputPin) == message.pinHash) {
                val decrypted = AES.decrypt(message.encodedText, inputPin)
                showDecryptedDialog(decrypted)
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        showDecryptedDialog(message.encodedText)
    }
}

messageList.adapter = adapter
messageList.layoutManager = LinearLayoutManager(requireContext())*/


/*  // 🔓 3. Show PIN Dialog + Decrypt Message
  fun showPinDialog(onPinEntered: (String) -> Unit) {
      val input = EditText(requireContext()).apply {
          inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
          hint = "Enter 4-digit PIN"
      }

      AlertDialog.Builder(requireContext())
          .setTitle("Decrypt Message")
          .setView(input)
          .setPositiveButton("Decrypt") { _, _ ->
              val pin = input.text.toString()
              if (pin.length == 4) onPinEntered(pin)
          }
          .setNegativeButton("Cancel", null)
          .show()
  }

  fun showDecryptedDialog(text: String) {
      AlertDialog.Builder(requireContext())
          .setTitle("Decrypted Message")
          .setMessage(text)
          .setPositiveButton("OK", null)
          .show()
  }*/


//🧠 2. Kotlin Logic: Sending Messages ~ Encrypting

//encryptSwitch.setOnCheckedChangeListener { _, isChecked ->
//    pinInput.visibility = if (isChecked) View.VISIBLE else View.GONE
//}
//
//sendButton.setOnClickListener {
//    val message = messageInput.text.toString()
//    val encrypt = encryptSwitch.isChecked
//    val pin = pinInput.text.toString()
//
//    val finalMessage = if (encrypt && pin.length == 4) {
//        AES.encrypt(message, pin)
//    } else {
//        message
//    }
//
//    val pinHash = if (encrypt && pin.length == 4) {
//        hashPin(pin)
//    } else {
//        null
//    }
//
//    // Send to Supabase or your backend
//    sendMessageToServer(finalMessage, pinHash)
//}

// 📩 3. Reading Messages ~ Decrypting

//fun onMessageClicked(message: Message) {
//    if (message.pinHash != null) {
//        showPinDialog { inputPin ->
//            if (hashPin(inputPin) == message.pinHash) {
//                val decrypted = AES.decrypt(message.encodedText, inputPin)
//                showDecryptedDialog(decrypted)
//            } else {
//                Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
//            }
//        }
//    } else {
//        showDecryptedDialog(message.encodedText) // plain text
//    }
//}


/* To toggle PIN visibility smoothly:
encryptSwitch.setOnCheckedChangeListener { _, isChecked ->
    TransitionManager.beginDelayedTransition(chatScreen)  // Animate layout changes
    pinInput.visibility = if (isChecked) View.VISIBLE else View.GONE
}*/


//// In your Activity to handle keyboard send button
//messageInput.setOnEditorActionListener { _, actionId, _ ->
//    if (actionId == EditorInfo.IME_ACTION_SEND) {
//        sendMessage()
//        true
//    } else false
//}
