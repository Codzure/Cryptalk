package com.codzure.cryptalk.di

import com.codzure.cryptalk.auth.AuthViewModel
import com.codzure.cryptalk.auth.UserRepository
import com.codzure.cryptalk.data.MessageRepository
import com.codzure.cryptalk.data.MessageRepositoryImpl
import com.codzure.cryptalk.viewmodels.ChatViewModel
import com.codzure.cryptalk.viewmodels.ChatsListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin DI module for the application
 */
val appModule = module {
    // Repositories
    single<MessageRepository> { MessageRepositoryImpl() }
    single { UserRepository(androidContext()) }
    
    // ViewModels
    viewModel { ChatsListViewModel(get()) }
    viewModel { ChatViewModel(get()) }
    viewModel { AuthViewModel(get()) }
}
