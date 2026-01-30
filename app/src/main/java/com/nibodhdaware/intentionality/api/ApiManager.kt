package com.nibodhdaware.intentionality.api

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// DataStore extension
val Context.apiDataStore: DataStore<Preferences> by preferencesDataStore(name = "api_preferences")

// API Models
@Serializable
data class SyncRequest(
    val title: String,
    val description: String,
    val rating: Int,
    val userAgent: String,
    val packageName: String? = null
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val id: String? = null,
    val error: String? = null
)

// API Interface
interface IntentionalityApi {
    @POST("sync")
    suspend fun sync(@Body request: SyncRequest): retrofit2.Response<SyncResponse>
}

// User data class to replace FirebaseUser
data class ApiUser(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)

object ApiManager {
    private const val BASE_URL = "https://intentionality.app/api/"
    
    // DataStore keys
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    private val USER_DISPLAY_NAME_KEY = stringPreferencesKey("user_display_name")
    private val USER_PHOTO_URL_KEY = stringPreferencesKey("user_photo_url")
    private val IS_SIGNED_IN_KEY = booleanPreferencesKey("is_signed_in")
    
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var api: IntentionalityApi
    
    // Initialize with context
    fun initialize(context: Context) {
        dataStore = context.apiDataStore
        
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("ApiManager", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Intentionality-Android/1.0")
                    .build()
                chain.proceed(request)
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                Json.asConverterFactory("application/json".toMediaType())
            )
            .build()
        
        api = retrofit.create(IntentionalityApi::class.java)
    }
    
    // Authentication methods (using DataStore for persistence)
    suspend fun signInWithGoogle(idToken: String): Result<ApiUser> {
        return try {
            // For anonymous API, we don't actually need to authenticate
            // But we'll store user info locally for consistency
            val userId = UUID.randomUUID().toString()
            
            dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = userId
                preferences[IS_SIGNED_IN_KEY] = true
            }
            
            val user = ApiUser(id = userId)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("ApiManager", "Google sign-in error", e)
            Result.failure(e)
        }
    }
    
    suspend fun signUpWithEmail(email: String, password: String): Result<ApiUser> {
        return try {
            // For anonymous API, we don't actually need to authenticate
            val userId = UUID.randomUUID().toString()
            
            dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = userId
                preferences[USER_EMAIL_KEY] = email
                preferences[IS_SIGNED_IN_KEY] = true
            }
            
            val user = ApiUser(id = userId, email = email)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("ApiManager", "Email sign-up error", e)
            Result.failure(e)
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<ApiUser> {
        return try {
            // For anonymous API, we don't actually need to authenticate
            val userId = UUID.randomUUID().toString()
            
            dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = userId
                preferences[USER_EMAIL_KEY] = email
                preferences[IS_SIGNED_IN_KEY] = true
            }
            
            val user = ApiUser(id = userId, email = email)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("ApiManager", "Email sign-in error", e)
            Result.failure(e)
        }
    }
    
    suspend fun resetPassword(email: String): Result<Unit> {
        // For anonymous API, this is a no-op
        return Result.success(Unit)
    }
    
    suspend fun signOut() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun updateUserProfile(displayName: String?, photoUrl: String?) {
        dataStore.edit { preferences ->
            if (displayName != null) preferences[USER_DISPLAY_NAME_KEY] = displayName
            if (photoUrl != null) preferences[USER_PHOTO_URL_KEY] = photoUrl
        }
    }
    
    suspend fun getCurrentUser(): ApiUser? {
        return try {
            val userId = dataStore.data.first()[USER_ID_KEY]
            val email = dataStore.data.first()[USER_EMAIL_KEY]
            val displayName = dataStore.data.first()[USER_DISPLAY_NAME_KEY]
            val photoUrl = dataStore.data.first()[USER_PHOTO_URL_KEY]
            val isSignedIn = dataStore.data.first()[IS_SIGNED_IN_KEY] ?: false
            
            if (isSignedIn && !userId.isNullOrEmpty()) {
                ApiUser(
                    id = userId,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ApiManager", "Error getting current user", e)
            null
        }
    }
    
    suspend fun isUserSignedIn(): Boolean {
        return try {
            dataStore.data.first()[IS_SIGNED_IN_KEY] ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    // Data Storage methods
    suspend fun saveAppEntry(
        appName: String,
        packageName: String,
        reason: String,
        dumbReason: String,
        sessionDuration: Double,
        url: String = ""
    ): Result<Unit> {
        return try {
            // Convert dumbReason to rating (1-5 scale)
            val rating = when (dumbReason) {
                "productive" -> 1
                "slightly_distracted" -> 2
                "pretty_distracted" -> 3
                "very_distracted" -> 4
                "extremely_distracted" -> 5
                else -> 3
            }
            
            val syncRequest = SyncRequest(
                title = appName,
                description = reason,
                rating = rating,
                userAgent = "Intentionality-Android/1.0",
                packageName = packageName
            )
            
            val response = api.sync(syncRequest)
            
            if (response.isSuccessful) {
                val syncResponse = response.body()
                if (syncResponse?.success == true) {
                    Log.d("ApiManager", "✅ Successfully synced entry: ${syncResponse.id}")
                    Result.success(Unit)
                } else {
                    val error = syncResponse?.error ?: "Unknown error"
                    Log.e("ApiManager", "❌ API error: $error")
                    Result.failure(Exception("API error: $error"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ApiManager", "❌ HTTP error ${response.code()}: $errorBody")
                Result.failure(Exception("HTTP error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ApiManager", "❌ Network error", e)
            Result.failure(e)
        }
    }
    
    // For API-based sync, we'll use local storage since the API is anonymous
    suspend fun getAppEntries(limit: Int = 100): Result<List<AppEntry>> {
        // Since the API is anonymous and doesn't provide user data retrieval,
        // we'll return empty list for now. In a real implementation, you might
        // want to add local storage or extend the API to support data retrieval.
        return Result.success(emptyList())
    }
    
    suspend fun getProductivityScore(timeRange: TimeRange = TimeRange.TODAY): Result<ProductivityStats> {
        // Since we can't retrieve data from the anonymous API, return empty stats
        return Result.success(
            ProductivityStats(
                totalEntries = 0,
                productiveCount = 0,
                distractedCount = 0,
                averageScore = 0.0,
                productivePercentage = 0.0,
                distractedPercentage = 0.0,
                totalTimeDistracted = 0.0,
                entries = emptyList()
            )
        )
    }
    
    // Sync monitored apps - using local storage since API is anonymous
    suspend fun syncMonitoredApps(apps: List<MonitoredAppData>): Result<Unit> {
        // For now, this is a no-op since the API is anonymous
        // In a real implementation, you might add local storage or extend the API
        Log.d("ApiManager", "✅ Monitored apps sync (local storage only): ${apps.size} apps")
        return Result.success(Unit)
    }
    
    suspend fun getMonitoredApps(): Result<List<MonitoredAppData>> {
        // Since we can't retrieve from the anonymous API, return empty list
        // In a real implementation, you would use local storage
        return Result.success(emptyList())
    }
}

// Data classes (keeping the same structure for compatibility)
data class AppEntry(
    val id: String,
    val appName: String,
    val packageName: String,
    val reason: String,
    val dumbReason: String,
    val sessionDuration: Double,
    val timestamp: String,
    val url: String,
    val userId: String,
    val userAgent: String = ""
)

data class MonitoredAppData(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean = true,
    val customIntention: String = "",
    val intervalMinutes: Int = 5
)

data class ProductivityStats(
    val totalEntries: Int,
    val productiveCount: Int,
    val distractedCount: Int,
    val averageScore: Double,
    val productivePercentage: Double,
    val distractedPercentage: Double,
    val totalTimeDistracted: Double,
    val entries: List<AppEntry>
)

enum class TimeRange {
    TODAY,
    WEEK,
    MONTH,
    ALL
}