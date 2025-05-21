package com.codzure.cryptalk.auth

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.codzure.cryptalk.R
import com.codzure.cryptalk.databinding.FragmentRegisterBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.ByteArrayOutputStream
import java.io.InputStream

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModel()
    
    // Variable to store the selected profile image as Base64 string
    private var profileImageBase64: String? = null
    
    // Activity result launcher for image selection
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    // Update the ImageView with the selected image
                    binding.profileImage.setImageURI(uri)
                    
                    // Convert the image to Base64
                    profileImageBase64 = convertImageToBase64(uri)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
        setupInputValidation()
        observeViewModel()
    }

    private fun setupListeners() {
        // Navigate back to login when login text is clicked
        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Set up image selection button
        binding.btnSelectImage.setOnClickListener {
            openImagePicker()
        }
        
        // Allow clicking on the profile image to select a new image
        binding.profileImage.setOnClickListener {
            openImagePicker()
        }

        // Register button click
        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                val fullName = binding.etName.text.toString().trim()
                val phoneNumber = binding.etPhoneNumber.text.toString().trim()
                val email = binding.etEmail.text.toString().trim().ifBlank { null }
                val password = binding.etPassword.text.toString()

                viewModel.register(fullName, phoneNumber, email, password, profileImageBase64)
                
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

    /**
     * Opens the image picker to select a profile image
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }
    
    /**
     * Converts an image URI to a Base64 encoded string
     */
    private fun convertImageToBase64(uri: Uri): String {
        val inputStream: InputStream = requireContext().contentResolver.openInputStream(uri) ?: return ""
        
        // Decode the image maintaining aspect ratio but limit size
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()
        
        // Calculate sample size to resize image to reasonable dimensions
        val maxDimension = 512 // Max width or height
        var sampleSize = 1
        if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
            val heightRatio = Math.round(options.outHeight.toFloat() / maxDimension.toFloat())
            val widthRatio = Math.round(options.outWidth.toFloat() / maxDimension.toFloat())
            sampleSize = Math.max(heightRatio, widthRatio)
        }
        
        // Decode with the sample size
        val newInputStream = requireContext().contentResolver.openInputStream(uri) ?: return ""
        val decodingOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodingOptions)
        newInputStream.close()
        
        // Convert to Base64
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
