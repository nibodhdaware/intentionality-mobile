package com.nibodhdaware.intentionality.ui.billing

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nibodhdaware.intentionality.billing.BillingManager
import com.revenuecat.purchases.Package

private const val TAG = "PaywallScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit,
    onPurchaseSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val offerings by BillingManager.offerings.collectAsState()
    val isLoading by BillingManager.isLoading.collectAsState()
    var selectedPackage by remember { mutableStateOf<Package?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    
    // Get packages
    val packages = offerings?.current?.availablePackages ?: emptyList()
    val monthlyPackage = packages.find { it.identifier == "\$rc_monthly" }
    val yearlyPackage = packages.find { it.identifier == "\$rc_annual" }
    val lifetimePackage = packages.find { it.identifier == "\$rc_lifetime" }
    
    // Auto-select yearly as best value
    LaunchedEffect(packages) {
        if (selectedPackage == null && yearlyPackage != null) {
            selectedPackage = yearlyPackage
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Go Premium",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Unlock all features",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Benefits list - minimal
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BenefitRow("Unlimited app monitoring")
                BenefitRow("Usage analytics & graphs")
                BenefitRow("Custom intention prompts per app")
                BenefitRow("Custom overlay repeat interval")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Plan options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Yearly - Best Value
                yearlyPackage?.let { pkg ->
                    PlanOption(
                        title = "Yearly",
                        price = pkg.product.price.formatted,
                        subtitle = "Save 33%",
                        isSelected = selectedPackage == pkg,
                        isBestValue = true,
                        onClick = { selectedPackage = pkg }
                    )
                }
                
                // Monthly
                monthlyPackage?.let { pkg ->
                    PlanOption(
                        title = "Monthly",
                        price = pkg.product.price.formatted,
                        subtitle = "Billed monthly",
                        isSelected = selectedPackage == pkg,
                        isBestValue = false,
                        onClick = { selectedPackage = pkg }
                    )
                }
                
                // Lifetime
                lifetimePackage?.let { pkg ->
                    PlanOption(
                        title = "Lifetime",
                        price = pkg.product.price.formatted,
                        subtitle = "One-time purchase",
                        isSelected = selectedPackage == pkg,
                        isBestValue = false,
                        onClick = { selectedPackage = pkg }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Purchase button
            Button(
                onClick = {
                    selectedPackage?.let { pkg ->
                        activity?.let { act ->
                            BillingManager.purchase(
                                activity = act,
                                packageToPurchase = pkg,
                                onSuccess = { 
                                    Log.d(TAG, "Purchase successful")
                                    onPurchaseSuccess()
                                },
                                onError = { error ->
                                    Log.e(TAG, "Purchase error: $error")
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                },
                enabled = selectedPackage != null && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Restore purchases
            TextButton(
                onClick = {
                    isRestoring = true
                    BillingManager.restorePurchases(
                        onSuccess = { customerInfo ->
                            isRestoring = false
                            val hasPro = customerInfo.entitlements.active[BillingManager.ENTITLEMENT_PRO] != null
                            if (hasPro) {
                                Toast.makeText(context, "Purchases restored!", Toast.LENGTH_SHORT).show()
                                onPurchaseSuccess()
                            } else {
                                Toast.makeText(context, "No previous purchases found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onError = { error ->
                            isRestoring = false
                            Toast.makeText(context, "Restore failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !isRestoring && !isLoading
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Restore Purchases",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Terms
            Text(
                text = "Subscriptions auto-renew unless cancelled 24 hours before renewal. Manage in Google Play settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PlanOption(
    title: String,
    price: String,
    subtitle: String,
    isSelected: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title and subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "BEST",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Price
            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
