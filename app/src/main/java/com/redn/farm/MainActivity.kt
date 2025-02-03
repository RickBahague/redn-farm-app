/*
 * Copyright (c) 2024 RedN Farm App Team
 * 
 * This file is part of RedN Farm App, licensed under the MIT License.
 * See the LICENSE file for details.
 *
 * Designers:
 * - Elion Bahague
 * - Eyo Bahague
 * - Rick Bahague
 *
 * Coders:
 * - Piggy, the RedN AI Coder
 * - Rick Bahague
 */

package com.redn.farm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.redn.farm.navigation.NavGraph
import com.redn.farm.ui.theme.FarmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FarmTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}