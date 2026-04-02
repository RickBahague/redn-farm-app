package com.redn.farm.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.security.Rbac
import com.redn.farm.ui.theme.FarmTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToOrders: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToAcquire: () -> Unit,
    onNavigateToRemittance: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToFarmOps: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    Scaffold(
        topBar = {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Yongy & Eyo's FARM",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
//                        Text(
//                            text = AppConfig.FARM_LOCATION,
//                            style = MaterialTheme.typography.bodyMedium
//                        )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                        if (isAdmin) {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                        IconButton(onClick = onNavigateToAbout) {
                            Icon(Icons.Default.Info, contentDescription = "About")
                        }
                        IconButton(
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Name at the top
//            Text(
//                text = AppConfig.APP_NAME,
//                style = MaterialTheme.typography.headlineMedium,
//                color = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.padding(bottom = 16.dp)
//            )

            // Menu tiles (2-column grid)
            val tiles = listOf(
                Triple("Orders", Icons.Default.ShoppingCart, onNavigateToOrders),
                Triple("Customers", Icons.Default.People, onNavigateToCustomers),
                Triple("Inventory", Icons.Default.Inventory, onNavigateToAcquire),
                Triple("Farm Ops", Icons.Default.Agriculture, onNavigateToFarmOps),
                Triple("Remittance", Icons.Default.Payments, onNavigateToRemittance),
                Triple("Employees", Icons.Default.Groups, onNavigateToEmployees),
                Triple("Products", Icons.Default.ListAlt, onNavigateToProducts),
                Triple("Export", Icons.Default.FileDownload, onNavigateToExport)
            )

            val allowedTitles = Rbac.dashboardTileTitles(Rbac.normalizeRole(userRole))
            val filteredTiles = tiles.filter { it.first in allowedTitles }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(filteredTiles) { tile ->
                    OutlinedCard(
                        onClick = tile.third,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tile.second,
                                contentDescription = tile.first,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tile.first,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

//            // App Description at the bottom
//            Text(
//                text = AppConfig.APP_DESC,
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                modifier = Modifier.padding(top = 16.dp),
//                textAlign = TextAlign.Center
//            )
        }
    }
}

enum class MenuItems(val title: String, val icon: ImageVector) {
    TAKE_ORDER("Take Order", Icons.Filled.ShoppingCart),
    ORDER_HISTORY("Order History", Icons.Filled.History),
    ACQUIRE_PRODUCE("Acquire Produce", Icons.Filled.Inventory),
    MANAGE_INPUTS("Manage Farm Inputs", Icons.Filled.Agriculture),
    MANAGE_EMPLOYEES("Manage Employees", Icons.Filled.Groups),
    MANAGE_PRODUCTS("Manage Product List", Icons.Filled.ListAlt),
    REMITTANCE("Remittance", Icons.Filled.Payments),
    MANAGE_CUSTOMERS("Manage Customers", Icons.Filled.People)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FarmTheme {
        MainScreen(
            onNavigateToOrders = {},
            onNavigateToProducts = {},
            onNavigateToCustomers = {},
            onNavigateToAcquire = {},
            onNavigateToRemittance = {},
            onNavigateToEmployees = {},
            onNavigateToFarmOps = {},
            onNavigateToExport = {},
            onNavigateToAbout = {},
            onNavigateToProfile = {},
            onNavigateToSettings = {},
            onLogout = {}
        )
    }
} 