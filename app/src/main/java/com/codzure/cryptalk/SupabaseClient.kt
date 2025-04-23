package com.codzure.cryptalk

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://mtcwiziyizuastaiuenf.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im10Y3dpeml5aXp1YXN0YWl1ZW5mIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQ4MDc0NjksImV4cCI6MjA2MDM4MzQ2OX0.inZCUt2iXk-w4nbhT6n0g88f2OeiZnn3EGPMTNC-ZiA"

        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}