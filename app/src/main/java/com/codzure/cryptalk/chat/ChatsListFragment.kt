package com.codzure.cryptalk.chat

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.codzure.cryptalk.R
import com.codzure.cryptalk.adapters.ConversationsAdapter
import com.codzure.cryptalk.auth.AuthViewModel
import com.codzure.cryptalk.databinding.FragmentChatsListBinding
import com.codzure.cryptalk.models.ConversationUI
import com.codzure.cryptalk.viewmodels.ChatsListViewModel
import com.google.android.material.snackbar.Snackbar
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
            // Mark conversation as read when clicked
            viewModel.markConversationAsRead(conversation.id)
            
            // Navigate to chat
            val action = ChatsListFragmentDirections.toChatFragment(
                conversation.userId, 
                conversation.userName
            )
            findNavController().navigate(action)
        }
        
        binding.chatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatsListFragment.adapter
            setHasFixedSize(true)
        }
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