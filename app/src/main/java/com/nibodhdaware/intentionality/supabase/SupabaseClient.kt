package com.nibodhdaware.intentionality.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SupabaseClientManager {
    private const val SUPABASE_URL = "https://wsmoiglcfnegltuuwhnh.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_ernPkyC6EdAA0d31G0X2sw_TZumZ_1l"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}

@Serializable
data class AppEntry(
    val id: Int? = null,
    @SerialName("app_name")
    val appName: String,
    @SerialName("package_name")
    val packageName: String,
    val reason: String,
    val rating: Int,
    val timestamp: String,
    @SerialName("user_id")
    val userId: String
)

