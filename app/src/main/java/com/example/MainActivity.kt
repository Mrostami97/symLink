package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.ITellApp
import com.example.ui.viewmodel.BridgeViewModel

class MainActivity : ComponentActivity() {

    private val bridgeViewModel: BridgeViewModel by viewModels()
    private var incomingSharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        extractSharedText(intent)
        setContent {
            ITellApp(
                viewModel = bridgeViewModel,
                incomingSharedText = incomingSharedText,
                onSharedTextConsumed = { incomingSharedText = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractSharedText(intent)
    }

    private fun extractSharedText(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let {
                incomingSharedText = it
            }
        }
    }
}
