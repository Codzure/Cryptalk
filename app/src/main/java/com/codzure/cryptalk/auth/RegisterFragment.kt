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
                val fullName = binding.etName.text.toString().trim()
                val phoneNumber = binding.etPhoneNumber.text.toString().trim()
                val email = binding.etEmail.text.toString().trim().ifBlank { null }
                val password = binding.etPassword.text.toString()

                viewModel.register(fullName, phoneNumber, email, password)
                
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
        
        // Phone number validation
        binding.etPhoneNumber.doOnTextChanged { text, _, _, _ ->
            binding.tilPhoneNumber.error = when {
                text.isNullOrBlank() -> "Phone number is required"
                !isValidPhoneNumber(text.toString()) -> "Enter a valid phone number"
                else -> null
            }
        }

        // Email validation (optional)
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            binding.tilEmail.error = if (!text.isNullOrBlank() && !isValidEmail(text.toString())) {
                "Enter a valid email address"
            } else null
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
        val phoneNumberValid = isValidPhoneNumber(binding.etPhoneNumber.text.toString())
        val emailText = binding.etEmail.text.toString()
        val emailValid = emailText.isBlank() || isValidEmail(emailText)
        val passwordValid = binding.etPassword.text.toString().length >= 6
        val confirmPasswordValid = binding.etPassword.text.toString() == binding.etConfirmPassword.text.toString()

        // Show errors for invalid fields
        if (!nameValid) binding.tilName.error = "Name is required"
        if (!phoneNumberValid) binding.tilPhoneNumber.error = "Enter a valid phone number"
        if (!emailValid) binding.tilEmail.error = "Enter a valid email address"
        if (!passwordValid) binding.tilPassword.error = "Password must be at least 6 characters"
        if (!confirmPasswordValid) binding.tilConfirmPassword.error = "Passwords don't match"

        return nameValid && phoneNumberValid && emailValid && passwordValid && confirmPasswordValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Basic validation - checks if it's 10-12 digits
        return phoneNumber.trim().matches(Regex("\\d{10,12}"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
