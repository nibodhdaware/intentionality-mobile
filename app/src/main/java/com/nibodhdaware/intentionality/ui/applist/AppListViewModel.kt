package com.nibodhdaware.intentionality.ui.applist

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nibodhdaware.intentionality.database.AppDatabase
import com.nibodhdaware.intentionality.database.IntentionLog
import com.nibodhdaware.intentionality.database.MonitoredApp
import com.nibodhdaware.intentionality.database.MonitoredAppRepository
import com.nibodhdaware.intentionality.service.AppMonitorService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.FlowPreview
import java.util.Calendar
import com.nibodhdaware.intentionality.billing.BillingManager
import com.nibodhdaware.intentionality.firebase.FirebaseManager // Import FirebaseManager

private const val TAG = "AppListViewModel"

data class AppInfo(
    val name: String,
    val packageName: String
)

@OptIn(FlowPreview::class)
class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MonitoredAppRepository
    private val database: AppDatabase = AppDatabase.getDatabase(application)
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )

    private val _isMonitoring = MutableStateFlow(
        sharedPrefs.getBoolean("is_monitoring", false)
    )
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Temporary selection state for app selection screen
    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    // Not needed anymore but kept for compatibility
    private val _hasMoreApps = MutableStateFlow(false)
    val hasMoreApps: StateFlow<Boolean> = _hasMoreApps.asStateFlow()

    val monitoredApps: StateFlow<List<MonitoredApp>>
    
    // All installed apps loaded once at startup
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()
    
    val filteredApps: StateFlow<List<AppInfo>>
    
    // Today's intention logs for the graph
    val todaysLogs: StateFlow<List<IntentionLog>>

    init {
        val monitoredAppDao = database.monitoredAppDao()
        repository = MonitoredAppRepository(monitoredAppDao, Dispatchers.IO) // Pass dispatcher

        monitoredApps = repository.allMonitoredApps
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
        // Get start of today for log filtering
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        todaysLogs = database.intentionLogDao().getTodaysLogs(startOfDay)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // Load ALL apps once at startup (in background, doesn't block UI)
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            
            val apps = resolveInfos.map { resolveInfo ->
                AppInfo(
                    name = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName
                )
            }.sortedBy { it.name.lowercase() }
            
            _allApps.value = apps
            _isInitialized.value = true
            Log.d(TAG, "Loaded ${apps.size} apps")

            // Fetch and merge from Firebase if user is premium and signed in
            if (BillingManager.hasProEntitlement() && FirebaseManager.isUserSignedIn()) {
                repository.fetchAndMergeFromFirebase()
            }
        }
        
        // Debounced filtering - waits 150ms after user stops typing
        // Also excludes apps that are already monitored
        filteredApps = combine(
            searchQuery.debounce(150),
            _allApps,
            monitoredApps
        ) { query, apps, monitored ->
            val monitoredPackages = monitored.map { it.packageName }.toSet()
            if (query.isBlank()) {
                emptyList() // Don't show any apps until user searches
            } else {
                apps.filter { 
                    it.name.contains(query, ignoreCase = true) && 
                    !monitoredPackages.contains(it.packageName)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
    
    // Not needed anymore but kept for compatibility
    fun loadNextPage() { }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    // Toggle app selection (for UI only, not database)
    // Returns false if free user has hit their limit
    fun toggleAppSelection(packageName: String): Boolean {
        val currentSelection = _selectedApps.value.toMutableSet()
        val isCurrentlySelected = currentSelection.contains(packageName)
        
        if (isCurrentlySelected) {
            // Always allow deselection
            currentSelection.remove(packageName)
            _selectedApps.value = currentSelection
            return true
        } else {
            // Check if free user can add more apps
            val currentMonitoredCount = monitoredApps.value.size
            val currentSelectedCount = currentSelection.size
            val totalAfterAdd = currentMonitoredCount + currentSelectedCount + 1
            
            if (!BillingManager.canAddMoreApps(currentMonitoredCount + currentSelectedCount)) {
                // Free user has hit their limit
                return false
            }
            
            currentSelection.add(packageName)
            _selectedApps.value = currentSelection
            return true
        }
    }
    
    // Initialize selection state from current monitored apps
    fun initializeSelectionFromMonitored() {
        viewModelScope.launch {
            try {
                val currentMonitored = monitoredApps.first().map { it.packageName }.toSet()
                _selectedApps.value = currentMonitored
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing selection", e)
            }
        }
    }
    
    // Clear selection state
    fun clearSelection() {
        _selectedApps.value = emptySet()
    }
    
    // Bulk add selected apps to local database (adds to existing, doesn't replace)
    suspend fun addSelectedAppsToMonitored() {
        try {
            val appsToAdd = _selectedApps.value.toList()
            
            // Add selected apps to local database (keep existing monitored apps)
            appsToAdd.forEach { packageName ->
                val appName = _allApps.value.find { it.packageName == packageName }?.name ?: packageName
                // Insert will be ignored if already exists due to primary key
                repository.insert(MonitoredApp(packageName, appName))
            }
            
            Log.d(TAG, "Added ${appsToAdd.size} new apps to monitoring")
            repository.syncToFirebase() // Sync to Firebase after adding apps
        } catch (e: Exception) {
            Log.e(TAG, "Error adding apps to monitoring", e)
        }
    }

    fun onAppChecked(packageName: String, isChecked: Boolean) {
        viewModelScope.launch {
            try {
                if (isChecked) {
                    val appName = _allApps.value.find { it.packageName == packageName }?.name ?: packageName
                    repository.insert(MonitoredApp(packageName, appName))
                } else {
                    repository.delete(MonitoredApp(packageName))
                }
                repository.syncToFirebase() // Sync to Firebase after app checked state changes
            } catch (e: Exception) {
                Log.e(TAG, "Error updating app checked state", e)
            }
        }
    }
    
    // Delete a monitored app (local only)
    fun deleteMonitoredApp(packageName: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting monitored app: $packageName")
                repository.delete(MonitoredApp(packageName))
                repository.syncToFirebase() // Sync to Firebase after deleting app
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting monitored app", e)
            }
        }
    }

    fun startMonitoring() {
        try {
            val context = getApplication<Application>()
            val intent = Intent(context, AppMonitorService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION.CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
            
            _isMonitoring.value = true
            sharedPrefs.edit().putBoolean("is_monitoring", true).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting monitoring", e)
        }
    }

    fun stopMonitoring() {
        try {
            val context = getApplication<Application>()
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
            
            _isMonitoring.value = false
            sharedPrefs.edit().putBoolean("is_monitoring", false).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping monitoring", e)
        }
    }
    
    suspend fun getMonitoredApp(packageName: String): MonitoredApp? {
        return withContext(Dispatchers.IO) {
            try {
                repository.getByPackageName(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting monitored app", e)
                null
            }
        }
    }
    
    suspend fun updateMonitoredApp(app: MonitoredApp) {
        withContext(Dispatchers.IO) {
            try {
                repository.update(app)
                repository.syncToFirebase() // Sync to Firebase after updating app
            } catch (e: Exception) {
                Log.e(TAG, "Error updating monitored app", e)
            }
        }
    }
}
