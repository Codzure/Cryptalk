package com.codzure.cryptalk.di

import com.codzure.cryptalk.api.AuthRepository
import com.codzure.cryptalk.api.AuthRepositoryImpl
import com.codzure.cryptalk.api.ChatRepository
import com.codzure.cryptalk.auth.AuthViewModel
import com.codzure.cryptalk.data.MessageRepository
import com.codzure.cryptalk.data.MessageRepositoryImpl
import com.codzure.cryptalk.viewmodels.ChatViewModel
import com.codzure.cryptalk.viewmodels.ChatsListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Main app dependency injection module
 */
val appModule = module {
    // Repositories
    single<MessageRepository> { MessageRepositoryImpl() } // Keep for backward compatibility
    single<AuthRepository> { AuthRepositoryImpl(get(), androidContext()) }
    single { ChatRepository(get(), get(), androidContext()) }
    
    // ViewModels
    viewModel { ChatsListViewModel(get()) }
    viewModel { ChatViewModel(get()) }
    viewModel { AuthViewModel(get()) }
}
