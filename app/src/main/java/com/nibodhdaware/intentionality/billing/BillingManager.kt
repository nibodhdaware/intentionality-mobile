package com.nibodhdaware.intentionality.billing

import android.app.Activity
import android.app.Application
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BillingManager - Complete RevenueCat SDK 9.19.0 Integration
 * 
 * Products configured in RevenueCat:
 * - monthly: $4.99/month subscription
 * - yearly: $49.99/year subscription  
 * - lifetime: $99.99 one-time purchase
 * 
 * Entitlement: "Intentionality Pro" - Grants access to unlimited apps and graph
 */
object BillingManager {
    
    private const val TAG = "BillingManager"
    
    // RevenueCat API key
    private const val REVENUECAT_API_KEY = "test_fIltteLnVhfNlcGDDTFVwCwVBBV"
    
    // Entitlement ID - matches RevenueCat dashboard
    const val ENTITLEMENT_PRO = "Intentionality Pro"
    
    // Product identifiers (create these in Google Play Console & RevenueCat)
    const val PRODUCT_MONTHLY = "monthly"
    const val PRODUCT_YEARLY = "yearly"
    const val PRODUCT_LIFETIME = "lifetime"
    
    // Free tier limits
    const val FREE_APP_LIMIT = 5
    
    // State flows for reactive UI updates
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
    
    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()
    
    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var isInitialized = false
    
    // SharedPreferences key for tracking first launch
    private const val PREFS_NAME = "billing_prefs"
    private const val KEY_HAS_RESTORED = "has_attempted_restore"
    
    /**
     * Initialize RevenueCat SDK - call this in Application.onCreate()
     * 
     * On reinstall, this will:
     * 1. Fetch customer info from RevenueCat (checks Google Play purchases)
     * 2. On first launch, silently attempt restore to recover any purchases
     */
    fun initialize(application: Application) {
        if (isInitialized) return
        
        try {
            // Set log level (use WARN or ERROR for production)
            Purchases.logLevel = LogLevel.DEBUG
            
            // Configure RevenueCat
            Purchases.configure(
                PurchasesConfiguration.Builder(application, REVENUECAT_API_KEY)
                    .build()
            )
            
            // Listen for customer info updates (purchases, restores, etc.)
            Purchases.sharedInstance.updatedCustomerInfoListener = 
                UpdatedCustomerInfoListener { customerInfo ->
                    updateCustomerInfo(customerInfo)
                    Log.d(TAG, "Customer info updated via listener")
                }
            
            isInitialized = true
            Log.d(TAG, "RevenueCat initialized successfully with API key")
            
            // Fetch initial data
            refreshCustomerInfo()
            fetchOfferings()
            
            // On first launch after install/reinstall, silently attempt to restore purchases
            // This recovers any purchases tied to the user's Google account
            val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_HAS_RESTORED, false)) {
                Log.d(TAG, "First launch detected, attempting silent restore...")
                silentRestore {
                    prefs.edit().putBoolean(KEY_HAS_RESTORED, true).apply()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RevenueCat", e)
            _error.value = "Failed to initialize billing: ${e.message}"
        }
    }
    
    /**
     * Fetch current customer info from RevenueCat
     */
    fun refreshCustomerInfo() {
        if (!isInitialized) {
            Log.w(TAG, "BillingManager not initialized")
            return
        }
        
        _isLoading.value = true
        
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateCustomerInfo(customerInfo)
                _isLoading.value = false
                Log.d(TAG, "Customer info refreshed successfully")
            }
            
            override fun onError(error: PurchasesError) {
                Log.e(TAG, "Error fetching customer info: ${error.message}")
                _error.value = error.message
                _isLoading.value = false
            }
        })
    }
    
    /**
     * Fetch available offerings (products/packages)
     */
    fun fetchOfferings() {
        if (!isInitialized) {
            Log.w(TAG, "BillingManager not initialized")
            return
        }
        
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                _offerings.value = offerings
                Log.d(TAG, "Offerings fetched: ${offerings.current?.availablePackages?.size ?: 0} packages available")
                
                // Log available packages for debugging
                offerings.current?.availablePackages?.forEach { pkg ->
                    Log.d(TAG, "Package: ${pkg.identifier} - ${pkg.product.price}")
                }
            }
            
            override fun onError(error: PurchasesError) {
                Log.e(TAG, "Error fetching offerings: ${error.message}")
                _error.value = error.message
            }
        })
    }
    
    /**
     * Purchase a package
     */
    fun purchase(
        activity: Activity,
        packageToPurchase: Package,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isInitialized) {
            onError("Billing not initialized")
            return
        }
        
        _isLoading.value = true
        
        Purchases.sharedInstance.purchase(
            PurchaseParams.Builder(activity, packageToPurchase).build(),
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    updateCustomerInfo(customerInfo)
                    _isLoading.value = false
                    Log.d(TAG, "Purchase successful: ${storeTransaction.orderId}")
                    onSuccess(customerInfo)
                }
                
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    _isLoading.value = false
                    if (userCancelled) {
                        Log.d(TAG, "Purchase cancelled by user")
                        onError("Purchase cancelled")
                    } else {
                        Log.e(TAG, "Purchase error: ${error.message}")
                        _error.value = error.message
                        onError(error.message)
                    }
                }
            }
        )
    }
    
    /**
     * Restore previous purchases
     */
    fun restorePurchases(
        onSuccess: (CustomerInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isInitialized) {
            onError("Billing not initialized")
            return
        }
        
        _isLoading.value = true
        
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateCustomerInfo(customerInfo)
                _isLoading.value = false
                
                val hasPro = customerInfo.entitlements.active[ENTITLEMENT_PRO] != null
                Log.d(TAG, "Restore successful, has Pro: $hasPro")
                onSuccess(customerInfo)
            }
            
            override fun onError(error: PurchasesError) {
                _isLoading.value = false
                Log.e(TAG, "Restore error: ${error.message}")
                _error.value = error.message
                onError(error.message)
            }
        })
    }
    
    /**
     * Silent restore - attempts to restore purchases without user interaction
     * Used on first launch to automatically recover purchases after reinstall
     */
    private fun silentRestore(onComplete: () -> Unit) {
        if (!isInitialized) {
            onComplete()
            return
        }
        
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateCustomerInfo(customerInfo)
                val hasPro = customerInfo.entitlements.active[ENTITLEMENT_PRO] != null
                Log.d(TAG, "Silent restore completed, has Pro: $hasPro")
                onComplete()
            }
            
            override fun onError(error: PurchasesError) {
                // Silent restore failed, but that's okay - user can manually restore later
                Log.w(TAG, "Silent restore failed: ${error.message}")
                onComplete()
            }
        })
    }
    
    /**
     * Update customer info and premium status
     */
    private fun updateCustomerInfo(info: CustomerInfo) {
        _customerInfo.value = info
        
        // Check if user has the Pro entitlement
        val proEntitlement = info.entitlements.active[ENTITLEMENT_PRO]
        _isPremium.value = proEntitlement != null
        
        Log.d(TAG, "Premium status updated: ${_isPremium.value}")
        
        // Log entitlement details for debugging
        proEntitlement?.let { entitlement ->
            Log.d(TAG, "Pro entitlement active:")
            Log.d(TAG, "  - Product ID: ${entitlement.productIdentifier}")
            Log.d(TAG, "  - Will renew: ${entitlement.willRenew}")
            Log.d(TAG, "  - Expiration: ${entitlement.expirationDate}")
        }
    }
    
    /**
     * Get specific package by type
     */
    fun getMonthlyPackage(): Package? = 
        _offerings.value?.current?.monthly
    
    fun getYearlyPackage(): Package? = 
        _offerings.value?.current?.annual
    
    fun getLifetimePackage(): Package? = 
        _offerings.value?.current?.lifetime
    
    /**
     * Get all available packages
     */
    fun getAllPackages(): List<Package> = 
        _offerings.value?.current?.availablePackages ?: emptyList()
    
    /**
     * Check if user can add more apps (premium or under/at free limit)
     * Free users can add up to FREE_APP_LIMIT apps (1 app)
     */
    fun canAddMoreApps(currentAppCount: Int): Boolean {
        return _isPremium.value || currentAppCount < FREE_APP_LIMIT
    }
    
    /**
     * Check if graph feature is available (premium only)
     */
    fun canAccessGraph(): Boolean = _isPremium.value
    
    /**
     * Check if user has active Pro subscription
     */
    fun hasProEntitlement(): Boolean = _isPremium.value
    
    /**
     * Get subscription expiration date (if applicable)
     */
    fun getExpirationDate(): String? {
        return _customerInfo.value?.entitlements?.active?.get(ENTITLEMENT_PRO)
            ?.expirationDate?.toString()
    }
    
    /**
     * Check if subscription will renew
     */
    fun willRenew(): Boolean {
        return _customerInfo.value?.entitlements?.active?.get(ENTITLEMENT_PRO)
            ?.willRenew == true
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Identify user (optional - for cross-platform user identification)
     */
    fun identifyUser(userId: String) {
        if (!isInitialized) return
        
        Purchases.sharedInstance.logIn(
            userId,
            object : com.revenuecat.purchases.interfaces.LogInCallback {
                override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                    updateCustomerInfo(customerInfo)
                    Log.d(TAG, "User identified: $userId, new user: $created")
                }
                
                override fun onError(error: PurchasesError) {
                    Log.e(TAG, "Error identifying user: ${error.message}")
                }
            }
        )
    }
    
    /**
     * Log out current user
     */
    fun logOut() {
        if (!isInitialized) return
        
        Purchases.sharedInstance.logOut(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateCustomerInfo(customerInfo)
                Log.d(TAG, "User logged out")
            }
            
            override fun onError(error: PurchasesError) {
                Log.e(TAG, "Error logging out: ${error.message}")
            }
        })
    }
}
