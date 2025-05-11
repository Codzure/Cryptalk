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
import com.codzure.cryptalk.databinding.FragmentRegisterBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupInputValidation()
        observeViewModel()
    }

    private fun setupListeners() {
        // Navigate back to login when login text is clicked
        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        // Register button click
        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                val name = binding.etName.text.toString()
                val email = binding.etEmail.text.toString()
                val password = binding.etPassword.text.toString()

                viewModel.register(name, email, password)
                
                // Show loading state
                binding.btnRegister.isEnabled = false
                binding.btnRegister.text = "Creating Account..."
            }
        }
    }

    private fun setupInputValidation() {
        // Name validation
        binding.etName.doOnTextChanged { text, _, _, _ ->
            binding.tilName.error = if (text.isNullOrBlank()) "Name is required" else null
        }

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
            
            // Also validate confirm password if it's not empty
            val confirmPassword = binding.etConfirmPassword.text.toString()
            if (confirmPassword.isNotBlank()) {
                binding.tilConfirmPassword.error = if (text.toString() != confirmPassword) {
                    "Passwords don't match"
                } else null
            }
        }

        // Confirm password validation
        binding.etConfirmPassword.doOnTextChanged { text, _, _, _ ->
            binding.tilConfirmPassword.error = when {
                text.isNullOrBlank() -> "Please confirm your password"
                text.toString() != binding.etPassword.text.toString() -> "Passwords don't match"
                else -> null
            }
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Idle -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "CREATE ACCOUNT"
                }
                is AuthState.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.btnRegister.text = "Creating Account..."
                }
                is AuthState.Success -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "CREATE ACCOUNT"
                    
                    // Navigate to chat list after successful registration
                    findNavController().navigate(R.id.action_registerFragment_to_chatsListFragment)
                }
                is AuthState.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "CREATE ACCOUNT"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val nameValid = binding.etName.text.toString().isNotBlank()
        val emailValid = isValidEmail(binding.etEmail.text.toString())
        val passwordValid = binding.etPassword.text.toString().length >= 6
        val passwordsMatch = binding.etPassword.text.toString() == binding.etConfirmPassword.text.toString()

        // Show errors for invalid fields
        if (!nameValid) binding.tilName.error = "Name is required"
        if (!emailValid) binding.tilEmail.error = "Enter a valid email address"
        if (!passwordValid) binding.tilPassword.error = "Password must be at least 6 characters"
        if (!passwordsMatch) binding.tilConfirmPassword.error = "Passwords don't match"

        return nameValid && emailValid && passwordValid && passwordsMatch
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
