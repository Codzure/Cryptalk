package com.codzure.cryptalk.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.FragmentLoginBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupInputValidation()
        observeViewModel()
    }

    private fun setupListeners() {
        // Login button
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                val email = binding.etEmail.text.toString()
                val password = binding.etPassword.text.toString()
                
                viewModel.login(email, password)
                
                // Show loading state
                binding.btnLogin.isEnabled = false
                binding.btnLogin.text = "SIGNING IN..."
            }
        }

        // Navigate to Registration
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Forgot password
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Password reset functionality will be implemented soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupInputValidation() {
        // Email validation
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            binding.tilEmail.error = when {
                text.isNullOrBlank() -> "Email is required"
                !isValidEmail(text.toString()) -> "Enter a valid email address"
                else -> null
            }
        }

        // Password validation
        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            binding.tilPassword.error = when {
                text.isNullOrBlank() -> "Password is required"
                text.length < 6 -> "Password must be at least 6 characters"
                else -> null
            }
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Idle -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "SIGN IN"
                }
                is AuthState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.text = "SIGNING IN..."
                }
                is AuthState.Success -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "SIGN IN"
                    
                    findNavController().navigate(R.id.action_loginFragment_to_chatsListFragment)
                }
                is AuthState.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "SIGN IN"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val emailValid = isValidEmail(binding.etEmail.text.toString())
        val passwordValid = binding.etPassword.text.toString().isNotBlank()

        // Show errors for invalid fields
        if (!emailValid) binding.tilEmail.error = "Enter a valid email address"
        if (!passwordValid) binding.tilPassword.error = "Password is required"

        return emailValid && passwordValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
