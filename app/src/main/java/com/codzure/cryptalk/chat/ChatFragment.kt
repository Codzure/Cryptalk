package com.codzure.cryptalk.chat

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
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.codzure.cryptalk.R
import com.codzure.cryptalk.adapters.MessageAdapter
import com.codzure.cryptalk.data.Message
import com.codzure.cryptalk.databinding.FragmentChatBinding
import com.codzure.cryptalk.dialogs.PinInputDialogFragment
import com.codzure.cryptalk.dialogs.PinMode
// Use a specific import to avoid ambiguity
import com.codzure.cryptalk.extensions.hideKeyboard as hideKeyboardExt
import com.codzure.cryptalk.viewmodels.ChatViewModel
import com.codzure.cryptalk.viewmodels.ChatsListViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MessageAdapter
    private val args: ChatFragmentArgs by navArgs()
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var isPolling = false
    
    // Inject ViewModels with Koin
    private val viewModel: ChatViewModel by viewModel()
    private val chatsListViewModel: ChatsListViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        enterTransition.duration = ANIMATION_DURATION_SHORT
        this.enterTransition = enterTransition
        this.exitTransition = enterTransition
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Make sure we rebuild the binding to include the swipeRefresh
        // This ensures the latest layout changes are reflected in the binding class
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupInputField()
        setupKeyboardVisibilityListener()
        observeViewModel()
        
        // Load conversation based on parameters
        val conversationId = args.conversationId
        
        // Try to get conversation from ChatsListViewModel
        val conversationResponse = chatsListViewModel.getConversationById(conversationId)
        
        if (conversationResponse != null) {
            // Load conversation with participant details
            viewModel.loadConversation(conversationId, conversationResponse.participant)
        } else if (args.userId != null && args.senderName != null) {
            // Legacy fallback - try to load by user ID
            showErrorSnackbar("Using legacy conversation loading...")
            viewModel.createConversationByUserId(args.userId!!, args.senderName!!)
        } else {
            // No valid parameters to load conversation
            showErrorSnackbar("Conversation not found. Please go back and try again.")
            findNavController().navigateUp()
        }

        // Handle window insets for status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Add swipe refresh for messages
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshMessages()
        }
        
        // Start message polling
        startMessagePolling()
    }
    
    private fun startMessagePolling() {
        if (isPolling) return
        isPolling = true
        
        viewLifecycleOwner.lifecycleScope.launch {
            while (isPolling && isAdded) {
                delay(POLLING_INTERVAL)
                viewModel.refreshMessages(silent = true)
            }
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe loading state
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (!isLoading) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe messages
            viewModel.messages.collect { messages ->
                adapter.submitList(messages)
                updateEmptyState(messages)
                if (messages.isNotEmpty()) {
                    binding.messageList.scrollToPosition(messages.size - 1)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe recipient
            viewModel.recipient.collect { user ->
                user?.let {
                    binding.toolbarTitle.text = it.fullName
                    binding.toolbarSubtitle.text = it.phoneNumber
                    binding.toolbarSubtitle.visibility = View.VISIBLE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe message sending state
            viewModel.messageSendingState.collect { state ->
                when(state) {
                    ChatViewModel.MessageSendState.SENDING -> {
                        binding.sendButton.isEnabled = false
                        binding.encryptionToggle.isEnabled = false
                        binding.sendProgressIndicator.visibility = View.VISIBLE
                    }
                    ChatViewModel.MessageSendState.SUCCESS -> {
                        binding.sendButton.isEnabled = true
                        binding.encryptionToggle.isEnabled = true
                        binding.sendProgressIndicator.visibility = View.GONE
                    }
                    ChatViewModel.MessageSendState.ERROR -> {
                        binding.sendButton.isEnabled = true
                        binding.encryptionToggle.isEnabled = true
                        binding.sendProgressIndicator.visibility = View.GONE
                    }
                    else -> {
                        binding.sendButton.isEnabled = true
                        binding.encryptionToggle.isEnabled = true
                        binding.sendProgressIndicator.visibility = View.GONE
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe errors
            viewModel.error.collect { errorMessage ->
                errorMessage?.let {
                    showErrorSnackbar(it)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbarTitle.text = args.senderName ?: "Chat"
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            emptyList(),
            viewModel.getCurrentUserId()
        ) { message, itemView ->
            if (message.pinHash != null) {
                showPinDialog(PinMode.DECRYPT) { inputPin ->
                    viewModel.decryptMessage(message, inputPin) { decryptedText, success ->
                        if (success) {
                            animateDecryption(itemView)
                            showDecryptedDialog(decryptedText ?: "Unable to decrypt message")
                        } else {
                            showErrorSnackbar("Incorrect PIN or message cannot be decrypted")
                        }
                    }
                }
            }
        }

        val controller =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
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

    private fun updateEmptyState(messages: List<Message>) {
        if (messages.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.messageList.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.messageList.visibility = View.VISIBLE
        }
    }

    private fun setupInputField() {
        binding.apply {
            sendButton.setOnClickListener {
                sendPlainMessage()
                hideKeyboardExt()
            }

            encryptionToggle.setOnClickListener {
                promptForEncryption()
            }

            messageInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    sendButton.isEnabled = s?.isNotEmpty() == true
                    encryptionToggle.isEnabled = s?.isNotEmpty() == true
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
                    }
                }
            }
        }
    }

    private fun sendPlainMessage() {
        val messageText = binding.messageInput.text.toString().trim()
        if (messageText.isEmpty()) return
        viewModel.sendMessage(messageText)
        binding.messageInput.text?.clear()
    }

    private fun setupKeyboardVisibilityListener() {
        val rootView = requireActivity().window.decorView.rootView
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            
            // Avoid NPE by checking if binding is still available
            if (_binding == null) return@OnGlobalLayoutListener
            
            if (keypadHeight > screenHeight * MIN_KEYBOARD_HEIGHT_RATIO) {
                // Keyboard is visible, adjust UI accordingly
                handleKeyboardShown(rect)
            } else {
                // Keyboard is hidden, reset scroll and translation
                binding.messageList.scrollTo(0, 0)
                binding.container.translationY = 0f
            }
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private fun handleKeyboardShown(rect: Rect) {
        // Avoid NPE by checking if binding is still available
        if (_binding == null) return
        
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
                binding.container.translationY = translationY
            }
        }
    }

    private fun promptForEncryption() {
        val messageText = binding.messageInput.text.toString().trim()
        if (messageText.isEmpty()) return

        MaterialAlertDialogBuilder(requireContext(), R.style.DialogSlideAnim)
            .setTitle("Encrypt this message?")
            .setMessage("Would you like to encrypt this message with a 4-digit PIN?")
            .setPositiveButton("Encrypt") { _, _ ->
                showPinDialog(PinMode.ENCRYPT) { pin ->
                    viewModel.sendMessage(messageText, pin)
                    binding.messageInput.text?.clear()
                    hideKeyboardExt()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            root.animate().alpha(1f).setDuration(ANIMATION_DURATION_SHORT).start()
        }

        dialog.show()
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.error_red, null))
            .setTextColor(resources.getColor(R.color.text_light, null))
            .show()
    }

    private fun animateDecryption(view: View) {
        val transition = MaterialContainerTransform().apply {
            startView = view
            endView = view
            duration = ANIMATION_DURATION_MEDIUM
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
        isPolling = false
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val MIN_KEYBOARD_HEIGHT_RATIO = 0.15
        private const val ANIMATION_DURATION_SHORT = 200L
        private const val ANIMATION_DURATION_MEDIUM = 400L
        private const val POLLING_INTERVAL = 5000L // 5 seconds
    }
}