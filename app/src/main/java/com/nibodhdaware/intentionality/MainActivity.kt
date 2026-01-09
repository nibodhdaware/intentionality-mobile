package com.nibodhdaware.intentionality

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.nibodhdaware.intentionality.ui.main.MainScreen
import com.nibodhdaware.intentionality.ui.theme.IntentionalityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntentionalityTheme(darkTheme = true) {
                // Go directly to MainScreen - no login, fully offline
                MainScreen(
                    onLogout = { /* No-op, no login/logout */ },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}