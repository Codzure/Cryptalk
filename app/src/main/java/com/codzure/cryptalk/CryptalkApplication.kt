package com.codzure.cryptalk

import android.app.Application
import com.codzure.cryptalk.di.appModule
import com.codzure.cryptalk.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 *
 * Application class for initializing dependencies
 */
class CryptalkApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin for Dependency Injection
        startKoin {
            // Use Android logger - Level.ERROR is recommended for production
            androidLogger(Level.DEBUG)
            // Declare Android context
            androidContext(this@CryptalkApplication)
            // Declare modules
            modules(listOf(appModule, networkModule))
        }
    }
}
