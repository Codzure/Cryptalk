package com.codzure.cryptalk.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.FragmentRegisterBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModel()
    private var selectedImageUri: Uri? = null
    
    // Image picker launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivProfileImage.setImageURI(it)
        }
    }

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
        observeViewModel()
    }
    
    private fun setupListeners() {
        // Handle back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Handle profile image selection
        binding.ivProfileImage.setOnClickListener { openImagePicker() }
        binding.tvAddPhoto.setOnClickListener { openImagePicker() }
        
        // Handle registration button click
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            
            if (validateInputs(fullName, username, email, password, confirmPassword)) {
                // Show loading state
                setLoading(true)
                
                // Attempt registration
                viewModel.register(
                    fullName = fullName,
                    username = username,
                    email = email,
                    password = password,
                    profileImageUri = selectedImageUri
                )
            }
        }
    }
    
    private fun openImagePicker() {
        pickImage.launch("image/*")
    }
    
    private fun observeViewModel() {
        // Observe authentication state
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            setLoading(false)
            
            when (state) {
                is AuthState.Success -> {
                    Toast.makeText(
                        context, 
                        "Registration successful! You can now login.", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Navigate back to login screen
                    findNavController().popBackStack()
                }
                is AuthState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    // Do nothing for loading state as it's handled by setLoading
                }
            }
        }
    }
    
    private fun validateInputs(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true
        
        // Reset all errors
        binding.tilFullName.error = null
        binding.tilUsername.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        
        // Validate full name
        if (fullName.isBlank()) {
            binding.tilFullName.error = "Full name is required"
            isValid = false
        }
        
        // Validate username
        if (username.isBlank()) {
            binding.tilUsername.error = "Username is required"
            isValid = false
        } else if (username.length < 3) {
            binding.tilUsername.error = "Username must be at least 3 characters"
            isValid = false
        }
        
        // Validate email
        if (email.isBlank()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            isValid = false
        }
        
        // Validate password
        if (password.isBlank()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        
        // Validate confirm password
        if (confirmPassword.isBlank()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }
        
        return isValid
    }
    
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Creating Account..."
        } else {
            binding.btnRegister.isEnabled = true
            binding.btnRegister.text = "CREATE ACCOUNT"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
