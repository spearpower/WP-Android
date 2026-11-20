package com.workspace.disciplehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.workspace.disciplehub.ui.AppRoot
import com.workspace.disciplehub.ui.theme.WPAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        takeRoute(intent)
    }

    /** A tapped notification carries "route" (cfg/app_push.php): the tab to open. */
    private fun takeRoute(intent: android.content.Intent?) {
        intent?.getStringExtra("route")?.let { (application as WpApp).pendingRoute.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        takeRoute(intent)
        setContent {
            WPAndroidTheme {
                AppRoot()
            }
        }
    }
}
