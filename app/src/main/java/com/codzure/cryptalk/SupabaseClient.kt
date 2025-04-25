package com.codzure.cryptalk

import android.util.Log
import com.codzure.cryptalk.data.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object SupabaseManager {


    private const val SUPABASE_URL = "https://mtcwiziyizuastaiuenf.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im10Y3dpeml5aXp1YXN0YWl1ZW5mIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQ4MDc0NjksImV4cCI6MjA2MDM4MzQ2OX0.inZCUt2iXk-w4nbhT6n0g88f2OeiZnn3EGPMTNC-ZiA"

    private const val TAG = "SupabaseManager"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY

        ) {
            install(Auth)
            install(Postgrest)
            install(GoTrue)
            install(Realtime)
            install(Storage)
        }

        val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

        // Channels for real-time updates
        var messagesChannel: RealtimeChannel? = null
        val messageListeners = mutableMapOf<String, (Message) -> Unit>()


        /**
         * Initialize Supabase connection
         * Should be called early in app lifecycle (e.g., Application.onCreate)
         */
        fun initialize() {
            coroutineScope.launch {
                try {
                    client.realtime.connect()
                    Log.d(TAG, "Supabase realtime connection established")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize Supabase: ${e.message}")
                }
            }
        }
    }
}