package com.nibodhdaware.intentionality.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

object FirebaseManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Authentication
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    fun isUserSignedIn(): Boolean = auth.currentUser != null
    
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Email/Password Authentication
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign up failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    // Data Storage - Updated to match Chrome extension schema
    suspend fun saveAppEntry(
        appName: String,
        packageName: String,
        reason: String,
        dumbReason: String,
        sessionDuration: Double,
        url: String = ""
    ): Result<Unit> {
        return try {
            val userId = getCurrentUser()?.uid ?: return Result.failure(Exception("User not signed in"))
            
            // Create Firestore Timestamp
            val timestamp = com.google.firebase.Timestamp.now()
            
            // Match the exact schema provided by user
            val entry = hashMapOf(
                "dumbReason" to dumbReason,           // e.g., "slightly_distracted"
                "reason" to reason,                   // e.g., "Anime life lessons"
                "sessionDuration" to sessionDuration, // e.g., 23.87 (number)
                "timestamp" to timestamp,             // Firestore Timestamp
                "url" to if (url.isNotEmpty()) url else "app://$packageName", // e.g., "https://www.youtube.com/..."
                "packageName" to packageName          // Additional field for app tracking
            )
            
            // Use Chrome extension collection structure: users/{userId}/activities
            firestore.collection("users")
                .document(userId)
                .collection("activities")
                .add(entry)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAppEntries(limit: Int = 100): Result<List<AppEntry>> {
        return try {
            val userId = getCurrentUser()?.uid ?: return Result.failure(Exception("User not signed in"))
            
            // Use Chrome extension collection structure: users/{userId}/activities
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("activities")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val entries = snapshot.documents.mapNotNull { doc ->
                try {
                    // Get timestamp - handle both Firestore Timestamp and String formats
                    val timestampObj = doc.get("timestamp")
                    val timestampString = when (timestampObj) {
                        is com.google.firebase.Timestamp -> {
                            // Convert Firestore Timestamp to readable format
                            val date = timestampObj.toDate()
                            SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a z", Locale.ENGLISH).apply {
                                timeZone = TimeZone.getDefault()
                            }.format(date)
                        }
                        is String -> timestampObj
                        else -> ""
                    }
                    
                    AppEntry(
                        id = doc.id,
                        appName = doc.getString("title") ?: doc.getString("app_name") ?: "", // Support both schemas
                        packageName = doc.getString("packageName") ?: doc.getString("package_name") ?: "",
                        reason = doc.getString("reason") ?: doc.getString("description") ?: "", // Support both schemas
                        dumbReason = doc.getString("dumbReason") ?: doc.getString("dumb_reason") ?: "",
                        sessionDuration = doc.getDouble("sessionDuration") ?: doc.getDouble("session_duration") ?: 0.0,
                        timestamp = timestampString,
                        url = doc.getString("url") ?: "",
                        userId = userId,
                        userAgent = doc.getString("userAgent") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error parsing activity entry", e)
                    null
                }
            }
            
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProductivityScore(timeRange: TimeRange = TimeRange.TODAY): Result<ProductivityStats> {
        return try {
            val entries = getAppEntries(1000).getOrNull() ?: emptyList()
            
            // Filter entries by time range
            val filteredEntries = filterEntriesByTimeRange(entries, timeRange)
            
            // Calculate productivity scores
            val productivityScores = mapOf(
                "productive" to 1.0,
                "slightly_distracted" to 0.5,
                "pretty_distracted" to 0.0,
                "very_distracted" to -0.5,
                "extremely_distracted" to -1.0
            )
            
            var totalScore = 0.0
            var productiveCount = 0
            var distractedCount = 0
            var totalTimeDistracted = 0.0
            
            filteredEntries.forEach { entry ->
                val score = productivityScores[entry.dumbReason] ?: 0.0
                totalScore += score
                
                if (score > 0) {
                    productiveCount++
                } else if (score < 0) {
                    distractedCount++
                    totalTimeDistracted += entry.sessionDuration
                }
            }
            
            val averageScore = if (filteredEntries.isNotEmpty()) totalScore / filteredEntries.size else 0.0
            val productivePercentage = if (filteredEntries.isNotEmpty()) 
                (productiveCount.toDouble() / filteredEntries.size) * 100 else 0.0
            val distractedPercentage = if (filteredEntries.isNotEmpty()) 
                (distractedCount.toDouble() / filteredEntries.size) * 100 else 0.0
            
            Result.success(
                ProductivityStats(
                    totalEntries = filteredEntries.size,
                    productiveCount = productiveCount,
                    distractedCount = distractedCount,
                    averageScore = averageScore,
                    productivePercentage = productivePercentage,
                    distractedPercentage = distractedPercentage,
                    totalTimeDistracted = totalTimeDistracted / 60, // Convert to minutes
                    entries = filteredEntries
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun filterEntriesByTimeRange(entries: List<AppEntry>, timeRange: TimeRange): List<AppEntry> {
        val calendar = Calendar.getInstance()
        val now = calendar.time
        
        return when (timeRange) {
            TimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time
                
                entries.filter { entry ->
                    val entryDate = parseTimestamp(entry.timestamp)
                    entryDate?.after(startOfDay) == true
                }
            }
            TimeRange.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.time
                
                entries.filter { entry ->
                    val entryDate = parseTimestamp(entry.timestamp)
                    entryDate?.after(weekAgo) == true
                }
            }
            TimeRange.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                val monthAgo = calendar.time
                
                entries.filter { entry ->
                    val entryDate = parseTimestamp(entry.timestamp)
                    entryDate?.after(monthAgo) == true
                }
            }
            TimeRange.ALL -> entries
        }
    }
    
    private fun parseTimestamp(timestamp: String): Date? {
        return try {
            // Try Chrome extension format first: "June 23, 2025 at 10:51:45 AM UTC+5:30"
            try {
                SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a z", Locale.ENGLISH).parse(timestamp)
            } catch (e: Exception) {
                // Fallback to ISO format for backward compatibility
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(timestamp)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Sync monitored apps to Firebase for cross-device synchronization
    suspend fun syncMonitoredApps(apps: List<MonitoredAppData>): Result<Unit> {
        return try {
            val userId = getCurrentUser()?.uid ?: return Result.failure(Exception("User not signed in"))
            
            // Convert apps to array of maps matching the Firebase structure
            val appsArray = apps.map { app ->
                hashMapOf(
                    "packageName" to app.packageName,
                    "appName" to app.appName,
                    "isInstalled" to app.isInstalled,
                    "customIntention" to app.customIntention,
                    "intervalMinutes" to app.intervalMinutes
                )
            }
            
            // Store in users/{userId}/settings/monitoredApps document
            val monitoredAppsData = hashMapOf(
                "apps" to appsArray,
                "lastUpdated" to com.google.firebase.Timestamp.now().toString()
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("monitoredApps")
                .set(monitoredAppsData)
                .await()
            
            Log.d("FirebaseManager", "✅ Synced ${apps.size} apps to Firebase")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Error syncing to Firebase", e)
            Result.failure(e)
        }
    }
    
    // Get monitored apps from Firebase
    suspend fun getMonitoredApps(): Result<List<MonitoredAppData>> {
        return try {
            val userId = getCurrentUser()?.uid ?: return Result.failure(Exception("User not signed in"))
            
            val doc = firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("monitoredApps")
                .get()
                .await()
            
            if (!doc.exists()) {
                Log.d("FirebaseManager", "No monitoredApps document found")
                return Result.success(emptyList())
            }
            
            @Suppress("UNCHECKED_CAST")
            val appsArray = doc.get("apps") as? List<Map<String, Any>>
            
            if (appsArray == null || appsArray.isEmpty()) {
                Log.d("FirebaseManager", "Empty apps array in Firebase")
                return Result.success(emptyList())
            }
            
            val apps = appsArray.mapNotNull { appMap ->
                val packageName = appMap["packageName"] as? String
                val appName = appMap["appName"] as? String
                val isInstalled = appMap["isInstalled"] as? Boolean ?: true
                val customIntention = appMap["customIntention"] as? String ?: ""
                val intervalMinutes = (appMap["intervalMinutes"] as? Long)?.toInt() ?: 5
                
                if (packageName != null) {
                    MonitoredAppData(
                        packageName = packageName,
                        appName = appName ?: packageName,
                        isInstalled = isInstalled,
                        customIntention = customIntention,
                        intervalMinutes = intervalMinutes
                    )
                } else {
                    null
                }
            }
            
            Log.d("FirebaseManager", "✅ Loaded ${apps.size} apps from Firebase")
            Result.success(apps)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Error loading from Firebase", e)
            Result.failure(e)
        }
    }
}

// Data classes - Updated to match Chrome extension schema
data class AppEntry(
    val id: String,
    val appName: String,           // Maps to 'title' in Firestore
    val packageName: String,       // Mobile-specific
    val reason: String,            // Maps to 'description' in Firestore
    val dumbReason: String,        // Mobile-specific
    val sessionDuration: Double,   // Mobile-specific
    val timestamp: String,         // Human-readable format
    val url: String,               // Mobile-specific
    val userId: String,
    val userAgent: String = ""     // Chrome extension field
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
