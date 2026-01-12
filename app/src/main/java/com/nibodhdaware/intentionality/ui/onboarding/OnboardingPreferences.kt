package com.nibodhdaware.intentionality.ui.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")

class OnboardingPreferences(private val context: Context) {
    
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val FEATURE_DISCOVERY_COMPLETED = booleanPreferencesKey("feature_discovery_completed")
        private val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
    }
    
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }
    
    val hasCompletedFeatureDiscovery: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FEATURE_DISCOVERY_COMPLETED] ?: false
        }
    
    val hasAskedNotificationPermission: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_PERMISSION_ASKED] ?: false
        }
    
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
    }
    
    suspend fun setFeatureDiscoveryCompleted() {
        context.dataStore.edit { preferences ->
            preferences[FEATURE_DISCOVERY_COMPLETED] = true
        }
    }
    
    suspend fun setNotificationPermissionAsked() {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_PERMISSION_ASKED] = true
        }
    }
    
    suspend fun resetOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = false
            preferences[FEATURE_DISCOVERY_COMPLETED] = false
            preferences[NOTIFICATION_PERMISSION_ASKED] = false
        }
    }
}
