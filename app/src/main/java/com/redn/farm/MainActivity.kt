package com.redn.farm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.redn.farm.navigation.NavGraph
import com.redn.farm.ui.components.SessionChecker
import com.redn.farm.ui.theme.FarmTheme
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge rendering so `imePadding()` works consistently.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            FarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    SessionChecker(navController = navController) {
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }
}