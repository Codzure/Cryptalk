package com.codzure.cryptalk.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Splash screen fragment that handles initial app loading and authentication check.
 * Routes the user to either login screen or chat list based on authentication status.
 */
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Simple animation for logo
        binding.ivLogo.alpha = 0f
        binding.ivLogo.animate().alpha(1f).setDuration(1000).start()
        binding.tvAppName.alpha = 0f
        binding.tvAppName.animate().alpha(1f).setDuration(1000).setStartDelay(300).start()
        binding.tvAppTagline.alpha = 0f
        binding.tvAppTagline.animate().alpha(1f).setDuration(1000).setStartDelay(500).start()
        
        // Check authentication and navigate accordingly after a short delay
        lifecycleScope.launch {
            delay(2000) // Show splash for at least 2 seconds
            checkAuthAndNavigate()
        }
    }
    
    private fun checkAuthAndNavigate() {
        if (authViewModel.isLoggedIn()) {
            // User is already logged in, navigate to chats list
            findNavController().navigate(R.id.action_splashFragment_to_chatsListFragment)
        } else {
            // User is not logged in, navigate to login screen
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
