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

package com.redn.farm.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RedN Farm App",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "A comprehensive farm management application built with modern Android development practices.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Team Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Development Team",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Designers",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    TeamMember(
                        name = "Elion Bahague",
                        role = "UI/UX Designer"
                    )
                    TeamMember(
                        name = "Eyo Bahague",
                        role = "UI/UX Designer"
                    )
                    TeamMember(
                        name = "Rick Bahague",
                        role = "UI/UX Designer"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Coders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    TeamMember(
                        name = "Piggy",
                        role = "RedN AI Coder"
                    )
                    TeamMember(
                        name = "Rick Bahague",
                        role = "Developer"
                    )
                }
            }

            // Technology Stack
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Technology Stack",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TechStackItem(
                        icon = Icons.Default.Code,
                        name = "Claude-Sonnet",
                        description = "Primary Large Language Model"
                    )
                    TechStackItem(
                        icon = Icons.Default.Code,
                        name = "Cursor",
                        description = "Primary AI Code Editor"
                    )
                    TechStackItem(
                        icon = Icons.Default.Code,
                        name = "Kotlin",
                        description = "Primary programming language"
                    )
                    TechStackItem(
                        icon = Icons.Default.Brush,
                        name = "Jetpack Compose",
                        description = "Modern UI toolkit"
                    )
                    TechStackItem(
                        icon = Icons.Default.Storage,
                        name = "Room Database",
                        description = "Local data persistence"
                    )
                    TechStackItem(
                        icon = Icons.Default.Architecture,
                        name = "MVVM Architecture",
                        description = "Clean and maintainable codebase"
                    )
                }
            }

            // License Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "License",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MIT License",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Copyright © 2024 RedN Farm App Team",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Contact Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Contact",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "For support and inquiries:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "farmapp@redn.asia",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamMember(
    name: String,
    role: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TechStackItem(
    icon: ImageVector,
    name: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 