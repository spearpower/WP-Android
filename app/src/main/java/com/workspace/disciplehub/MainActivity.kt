package com.workspace.disciplehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.workspace.disciplehub.ui.AppRoot
import com.workspace.disciplehub.ui.theme.WPAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WPAndroidTheme {
                AppRoot()
            }
        }
    }
}
