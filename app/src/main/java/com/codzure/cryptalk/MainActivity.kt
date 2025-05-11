package com.codzure.cryptalk

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.codzure.cryptalk.auth.AuthViewModel
import com.codzure.cryptalk.databinding.ActivityMainBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val authViewModel: AuthViewModel by viewModel()
    private var isAuthReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate
        val splashScreen = installSplashScreen()
        
        // Keep splash screen visible until authentication check completes
        splashScreen.setKeepOnScreenCondition { !isAuthReady }
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()
        enableSoftKeyboardAdjustResize()
        setupNavigation()
        
        // Check authentication state and navigate accordingly
        authViewModel.currentUser.observe(this, Observer { user ->
            // Mark auth check as complete to dismiss splash screen
            isAuthReady = true
            
            // If this is initial app start (savedInstanceState == null), navigate based on auth state
            if (savedInstanceState == null) {
                if (user != null) {
                    // User is logged in, navigate to chats list
                    navController.navigate(R.id.chatsListFragment)
                }
                // If user is null, we'll stay at the login fragment (our start destination)
            }
        })
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun enableSoftKeyboardAdjustResize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}