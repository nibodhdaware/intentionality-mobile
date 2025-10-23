package com.nibodhdaware.intentionality.supabase

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SupabaseRepository {
    private val client = SupabaseClientManager.client

    suspend fun saveAppEntry(
        appName: String,
        packageName: String,
        reason: String,
        rating: Int,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            val entry = mapOf(
                "app_name" to appName,
                "package_name" to packageName,
                "reason" to reason,
                "rating" to rating,
                "timestamp" to timestamp,
                "user_id" to userId
            )

            client.from("app_entries").insert(entry)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getUserId(): String? = withContext(Dispatchers.IO) {
        try {
            client.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            null
        }
    }
}

