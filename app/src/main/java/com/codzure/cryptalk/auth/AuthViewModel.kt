package com.codzure.cryptalk.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codzure.cryptalk.api.AuthRepository
import com.codzure.cryptalk.data.User
import kotlinx.coroutines.launch

/**
 * ViewModel for handling authentication related logic.
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    init {
        // Check if user is already logged in
        _currentUser.value = authRepository.getCurrentUser()
    }
    
    /**
     * Attempts to log in a user with the provided credentials.
     * 
     * @param phoneNumber The user's phone number (used as username)
     * @param password The user's password
     */
    fun login(phoneNumber: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = authRepository.login(phoneNumber, password)
            
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }
    
    /**
     * Registers a new user with the provided details.
     * 
     * @param fullName User's full name
     * @param phoneNumber User's phone number (used as username)
     * @param email User's email (optional)
     * @param password User's password
     */
    fun register(
        fullName: String,
        phoneNumber: String,
        email: String?,
        password: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = authRepository.register(
                username = phoneNumber,
                fullName = fullName,
                phoneNumber = phoneNumber,
                email = email,
                password = password
            )
            
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }
    
    /**
     * Logs out the current user.
     */
    fun logout() {
        authRepository.logout()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }
    
    /**
     * Resets the authentication state to Idle.
     * This can be used to clear error states or prepare for a new auth attempt.
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
