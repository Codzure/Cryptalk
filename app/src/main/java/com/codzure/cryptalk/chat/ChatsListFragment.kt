package com.codzure.cryptalk.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.codzure.cryptalk.R
import com.codzure.cryptalk.adapters.ConversationsAdapter
import com.codzure.cryptalk.adapters.UserSearchAdapter
import com.codzure.cryptalk.auth.AuthViewModel
import com.codzure.cryptalk.data.displayName
import com.codzure.cryptalk.databinding.FragmentChatsListBinding
import com.codzure.cryptalk.models.ConversationUI
import com.codzure.cryptalk.viewmodels.ChatsListViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChatsListFragment : Fragment() {

    private var _binding: FragmentChatsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ConversationsAdapter
    
    // Inject ViewModels with Koin
    private val viewModel: ChatsListViewModel by viewModel()
    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Enable options menu
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupAddConversationButton()
        observeViewModel()
        
        // Update UI with current user info
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Set toolbar title to include username if available
                activity?.title = "Chats (${it.displayName()})"
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                handleLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun handleLogout() {
        authViewModel.logout()
        // Navigate back to login screen
        findNavController().navigate(R.id.action_chatsListFragment_to_loginFragment)
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
            // Observe conversations
            viewModel.conversations.collect { conversations ->
                updateUI(conversations)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe errors
            viewModel.error.collect { errorMessage ->
                errorMessage?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                        .setAction("Dismiss") { viewModel.clearError() }
                        .show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        // Initialize adapter with conversation click handler
        adapter = ConversationsAdapter { conversation ->
            // Get the full conversation response
            val conversationResponse = viewModel.getConversationById(conversation.id)
            
            if (conversationResponse != null) {
                // Navigate to chat with conversation ID
                val action = ChatsListFragmentDirections.toChatFragment(
                    conversation.id,
                    conversationResponse.participant.fullName,
                    conversationResponse.participant.id
                )
                findNavController().navigate(action)
            } else {
                Snackbar.make(binding.root, "Conversation details not found", Snackbar.LENGTH_SHORT).show()
            }
        }
        
        binding.chatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatsListFragment.adapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadConversations()
        }
    }
    
    private fun setupAddConversationButton() {
        binding.fabNewChat.setOnClickListener {
            showUserSearchDialog()
        }
    }
    
    private fun showUserSearchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_user, null)
        val searchInput = dialogView.findViewById<TextInputEditText>(R.id.searchInput)
        val usersList = dialogView.findViewById<RecyclerView>(R.id.usersList)
        val emptyResultsText = dialogView.findViewById<TextView>(R.id.emptyResultsText)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Find someone to chat with")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()
            
        // Configure RecyclerView
        usersList.layoutManager = LinearLayoutManager(requireContext())
        val userAdapter = UserSearchAdapter { user ->
            // When user is selected, close dialog and navigate to chat
            dialog.dismiss()
            if (user.username.isNotBlank()) {
                // Create a new conversation with this user via ChatsListViewModel
                viewModel.createConversation(user.id)
                
                // Navigate to chat screen
                val action = ChatsListFragmentDirections.toChatFragment(
                    user.id,
                    user.fullName,
                    user.id
                )
                findNavController().navigate(action)
            } else {
                Snackbar.make(binding.root, "Cannot start chat - invalid username", Snackbar.LENGTH_SHORT).show()
            }
        }
        usersList.adapter = userAdapter
        
        // Configure search functionality
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank() && s.length >= 3) {
                    // Show loading indicator
                    emptyResultsText.visibility = View.GONE
                    usersList.visibility = View.GONE
                    
                    // Search users via repository
                    viewModel.searchUsers(s.toString()) { users ->
                        if (users.isEmpty()) {
                            emptyResultsText.visibility = View.VISIBLE
                            usersList.visibility = View.GONE
                        } else {
                            emptyResultsText.visibility = View.GONE
                            usersList.visibility = View.VISIBLE
                            userAdapter.submitList(users)
                        }
                    }
                }
            }
        })
        
        dialog.show()
    }
    
    private fun updateUI(conversations: List<ConversationUI>) {
        if (conversations.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.chatsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.chatsRecyclerView.visibility = View.VISIBLE
            adapter.submitConversations(conversations)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}