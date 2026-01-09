package com.nibodhdaware.intentionality.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class FeatureHighlight {
    SETTINGS_BUTTON,
    ADD_APPS_FAB,
    START_MONITORING,
    COMPLETE
}

data class HighlightInfo(
    val title: String,
    val description: String
)

val featureHighlights = mapOf(
    FeatureHighlight.SETTINGS_BUTTON to HighlightInfo(
        title = "Settings",
        description = "Manage permissions and app preferences"
    ),
    FeatureHighlight.ADD_APPS_FAB to HighlightInfo(
        title = "Add Apps",
        description = "Tap here to select apps you want to be more intentional about"
    ),
    FeatureHighlight.START_MONITORING to HighlightInfo(
        title = "Start Monitoring",
        description = "Once you've added apps, tap here to begin. You'll be prompted before opening monitored apps."
    )
)

@Composable
fun FeatureDiscoveryOverlay(
    currentHighlight: FeatureHighlight,
    highlightBounds: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (currentHighlight == FeatureHighlight.COMPLETE || highlightBounds == null) return
    
    val info = featureHighlights[currentHighlight] ?: return
    
    // Pulsing animation for the highlight circle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Consume clicks */ }
    ) {
        // Semi-transparent overlay with cutout using clipPath
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Calculate spotlight area with padding
            val padding = 12.dp.toPx()
            val cornerRadius = 12.dp.toPx()
            val spotlightRect = Rect(
                left = highlightBounds.left - padding,
                top = highlightBounds.top - padding,
                right = highlightBounds.right + padding,
                bottom = highlightBounds.bottom + padding
            )
            
            // Create a path for the spotlight cutout
            val cutoutPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = spotlightRect,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
            }
            
            // Draw the dark overlay with the cutout
            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.8f),
                    size = size
                )
            }
            
            // Draw pulsing border around spotlight
            val pulseOffset = (pulseScale - 1f) * 6.dp.toPx()
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(spotlightRect.left - pulseOffset, spotlightRect.top - pulseOffset),
                size = Size(spotlightRect.width + pulseOffset * 2, spotlightRect.height + pulseOffset * 2),
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Tooltip card - centered on screen
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                    
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper to track element positions
class HighlightCoordinatesState {
    var settingsButton: Rect? by mutableStateOf(null)
    var startMonitoringButton: Rect? by mutableStateOf(null)
    var addAppsFab: Rect? by mutableStateOf(null)
    
    fun getBounds(highlight: FeatureHighlight): Rect? {
        return when (highlight) {
            FeatureHighlight.SETTINGS_BUTTON -> settingsButton
            FeatureHighlight.ADD_APPS_FAB -> addAppsFab
            FeatureHighlight.START_MONITORING -> startMonitoringButton
            FeatureHighlight.COMPLETE -> null
        }
    }
}

fun Modifier.trackHighlight(
    state: HighlightCoordinatesState,
    highlight: FeatureHighlight
): Modifier = this.onGloballyPositioned { coordinates ->
    val position = coordinates.positionInRoot()
    val size = coordinates.size
    val bounds = Rect(
        left = position.x,
        top = position.y,
        right = position.x + size.width,
        bottom = position.y + size.height
    )
    when (highlight) {
        FeatureHighlight.SETTINGS_BUTTON -> state.settingsButton = bounds
        FeatureHighlight.ADD_APPS_FAB -> state.addAppsFab = bounds
        FeatureHighlight.START_MONITORING -> state.startMonitoringButton = bounds
        FeatureHighlight.COMPLETE -> {}
    }
}
