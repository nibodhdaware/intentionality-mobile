package com.nibodhdaware.intentionality.ui.applist

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nibodhdaware.intentionality.database.AppDatabase
import com.nibodhdaware.intentionality.database.MonitoredApp
import com.nibodhdaware.intentionality.database.MonitoredAppRepository
import com.nibodhdaware.intentionality.service.AppMonitorService
import com.nibodhdaware.intentionality.supabase.SupabaseClientManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

data class UserProfile(
    val name: String?,
    val email: String?,
    val photoUrl: String?
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MonitoredAppRepository
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )
    private val supabase = SupabaseClientManager.client

    private val _isMonitoring = MutableStateFlow(
        sharedPrefs.getBoolean("is_monitoring", false)
    )
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val monitoredApps: StateFlow<List<String>>
    val installedApps: StateFlow<List<AppInfo>>
    val filteredApps: StateFlow<List<AppInfo>>

    init {
        val monitoredAppDao = AppDatabase.getDatabase(application).monitoredAppDao()
        repository = MonitoredAppRepository(monitoredAppDao)

        monitoredApps = repository.allMonitoredApps.map { list ->
            list.map { it.packageName }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        installedApps = kotlinx.coroutines.flow.flow {
            val pm = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, 0)
                .map {
                    AppInfo(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = it.loadIcon(pm)
                    )
                }
                .sortedBy { it.name.lowercase() }
            emit(apps)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
        filteredApps = combine(installedApps, searchQuery) { apps, query ->
            if (query.isBlank()) {
                apps
            } else {
                apps.filter { it.name.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val session = supabase.auth.currentSessionOrNull()
                val user = session?.user
                
                Log.d("AppListViewModel", "Loading user profile...")
                Log.d("AppListViewModel", "Session: $session")
                Log.d("AppListViewModel", "User: $user")
                Log.d("AppListViewModel", "User metadata: ${user?.userMetadata}")
                
                val name = user?.userMetadata?.get("full_name") as? String 
                    ?: user?.userMetadata?.get("name") as? String
                val email = user?.email
                val photoUrl = user?.userMetadata?.get("avatar_url") as? String
                    ?: user?.userMetadata?.get("picture") as? String
                
                Log.d("AppListViewModel", "Extracted name: $name")
                Log.d("AppListViewModel", "Extracted email: $email")
                Log.d("AppListViewModel", "Extracted photoUrl: $photoUrl")
                
                _userProfile.value = UserProfile(
                    name = name,
                    email = email,
                    photoUrl = photoUrl
                )
                
                Log.d("AppListViewModel", "User profile set: ${_userProfile.value}")
            } catch (e: Exception) {
                Log.e("AppListViewModel", "Error loading user profile", e)
                e.printStackTrace()
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onAppChecked(packageName: String, isChecked: Boolean) {
        viewModelScope.launch {
            if (isChecked) {
                repository.insert(MonitoredApp(packageName))
            } else {
                repository.delete(MonitoredApp(packageName))
            }
        }
    }

    fun startMonitoring() {
        val context = getApplication<Application>()
        val intent = Intent(context, AppMonitorService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
        
        _isMonitoring.value = true
        sharedPrefs.edit().putBoolean("is_monitoring", true).apply()
    }

    fun stopMonitoring() {
        val context = getApplication<Application>()
        val intent = Intent(context, AppMonitorService::class.java)
        context.stopService(intent)
        
        _isMonitoring.value = false
        sharedPrefs.edit().putBoolean("is_monitoring", false).apply()
    }
}