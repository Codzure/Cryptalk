package com.codzure.cryptalk

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://ysmwaiaubqagigdcmsqk.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlzbXdhaWF1YnFhZ2lnZGNtc3FrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDY1MjMxODksImV4cCI6MjA2MjA5OTE4OX0.5jky03SLI3HCgjJvIls3FFdtdUUr5zIHfhZroIn8NVg"
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}