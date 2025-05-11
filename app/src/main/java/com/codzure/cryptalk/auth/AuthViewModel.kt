package com.codzure.cryptalk.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel for handling authentication operations including login, registration,
 * and tracking authentication state.
 */
class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {
    
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    init {
        // Check if user is already logged in
        _currentUser.value = userRepository.getCurrentUser()
    }
    
    /**
     * Attempts to log in a user with the provided credentials.
     * 
     * @param email The user's email address
     * @param password The user's password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            userRepository.login(email, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Login failed")
                }
        }
    }
    
    /**
     * Registers a new user with the provided information.
     * 
     * @param name The user's full name
     * @param email The user's email address
     * @param password The user's password
     */
    fun register(
        name: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            userRepository.register(name, email, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Registration failed")
                }
        }
    }
    
    /**
     * Logs out the current user.
     */
    fun logout() {
        userRepository.logout()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }
    
    /**
     * Resets the authentication state to idle.
     * Should be called when navigating away from auth screens.
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
