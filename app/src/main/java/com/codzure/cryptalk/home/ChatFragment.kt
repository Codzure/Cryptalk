package com.codzure.cryptalk.home

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.FragmentChatBinding
import com.codzure.cryptalk.dialogs.PinInputDialogFragment
import com.codzure.cryptalk.dialogs.PinMode
import com.codzure.cryptalk.extensions.AESAlgorithm.AES
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        enterTransition.duration = 300
        this.enterTransition = enterTransition
        this.exitTransition = enterTransition
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
        setupKeyboardVisibilityListener()

        // Handle window insets for status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            Log.d("ChatFragment", "Status bar top: ${systemBars.top}")
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(messages) { message, itemView ->
            if (message.pinHash != null) {
                showPinDialog(PinMode.DECRYPT) { inputPin ->
                    if (hashPin(inputPin) == message.pinHash) {
                        try {
                            val decrypted = AES.decrypt(message.encodedText, inputPin)
                            animateDecryption(itemView)
                            showDecryptedDialog(decrypted)
                        } catch (e: Exception) {
                            showErrorSnackbar("Decryption failed. Please try again.")
                        }
                    } else {
                        showErrorSnackbar("Incorrect PIN. Please try again.")
                    }
                }
            }
        }

        val controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        binding.messageList.layoutAnimation = controller
        binding.messageList.scheduleLayoutAnimation()

        binding.messageList.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
                reverseLayout = false
            }
            adapter = this@ChatFragment.adapter
            setHasFixedSize(true)
        }

        binding.messageList.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
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

            // Scroll RecyclerView if there are items
            messageInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && adapter.itemCount > 0) {
                    messageList.post {
                        messageList.smoothScrollToPosition(adapter.itemCount - 1)
                        Log.d(
                            "ChatFragment",
                            "Scrolled RecyclerView to position: ${adapter.itemCount - 1}"
                        )
                    }
                }
            }
        }
    }

    private fun setupKeyboardVisibilityListener() {
        val rootView = binding.root
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible, try scrolling RecyclerView
                binding.messageList.post {
                    val inputRect = Rect()
                    binding.container.getGlobalVisibleRect(inputRect)
                    val scrollY = inputRect.bottom - rect.bottom
                    if (scrollY > 0) {
                        binding.messageList.scrollBy(0, scrollY)
                    }
                    // Fallback: Translate MaterialCardView up
                    if (inputRect.bottom > rect.bottom) {
                        val translationY = (rect.bottom - inputRect.bottom).toFloat()
                        binding.inputWrapper.translationY = translationY
                    }
                }
            } else {
                // Keyboard is hidden, reset scroll and translation
                binding.messageList.scrollTo(0, 0)
                binding.inputWrapper.translationY = 0f
            }
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private fun promptForEncryption() {
        val messageText = binding.messageInput.text.toString().trim()
        if (messageText.isEmpty()) return

        MaterialAlertDialogBuilder(requireContext(), R.style.DialogSlideAnim)
            .setTitle("Encrypt this message?")
            .setMessage("Would you like to encrypt this message with a 4-digit PIN?")
            .setPositiveButton("Encrypt") { _, _ ->
                showPinDialog(PinMode.ENCRYPT) { pin ->
                    sendMessage(messageText, pin)
                }
            }
            .setNegativeButton("Send Plain") { _, _ ->
                sendMessage(messageText, null)
            }
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
                sender = "Leonard Mutugi",
                encodedText = finalMessage,
                senderNumber = "1234567890",
                pinHash = pinHash,
                isEncrypted = pin != null,
                timestamp = System.currentTimeMillis()
            )

            messages.add(newMessage)
            adapter.notifyItemInserted(messages.size - 1)
            binding.messageList.smoothScrollToPosition(messages.size - 1)
            binding.messageInput.text?.clear()

            // Simulate reply after 1.5 seconds
            binding.messageList.postDelayed({
                simulateReceiverReply()
            }, 1500)

        } catch (e: Exception) {
            showErrorSnackbar("Encryption failed: ${e.message}")
        }
    }

    private fun simulateReceiverReply() {
        val replyMessage = Message(
            id = System.currentTimeMillis().toString(),
            sender = "Alice Wonderland",
            encodedText = "Got your message!",
            senderNumber = "9876543210",
            pinHash = null,
            isEncrypted = false,
            timestamp = System.currentTimeMillis()
        )

        messages.add(replyMessage)
        adapter.notifyItemInserted(messages.size - 1)
        binding.messageList.smoothScrollToPosition(messages.size - 1)
    }

    private fun showPinDialog(mode: PinMode, onPinEntered: (String) -> Unit) {
        PinInputDialogFragment.newInstance(mode, onPinEntered)
            .show(parentFragmentManager, "PinDialog")
    }

    private fun showDecryptedDialog(text: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_decrypted_message, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogSlideAnim)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)

        dialogView.findViewById<TextView>(R.id.message).text = text
        dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val root = dialogView.parent as View
            root.alpha = 0f
            root.animate().alpha(1f).setDuration(300).start()
        }

        dialog.show()
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

    private fun animateDecryption(view: View) {
        val transition = MaterialContainerTransform().apply {
            startView = view
            endView = view
            duration = 500
            scrimColor = Color.TRANSPARENT
            startShapeAppearanceModel = ShapeAppearanceModel().withCornerSize(0f)
            endShapeAppearanceModel = ShapeAppearanceModel().withCornerSize(32f)
        }

        TransitionManager.beginDelayedTransition(view.parent as ViewGroup, transition)
        view.setBackgroundColor(Color.WHITE)
    }

    override fun onDestroyView() {
        globalLayoutListener?.let {
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        super.onDestroyView()
        _binding = null
    }
}