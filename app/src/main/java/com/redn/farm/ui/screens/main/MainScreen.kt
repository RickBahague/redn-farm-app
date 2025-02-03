package com.redn.farm.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redn.farm.config.AppConfig
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
    onNavigateToAbout: () -> Unit
) {
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
                                style = MaterialTheme.typography.headlineMedium.copy(
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
                        IconButton(onClick = onNavigateToAbout) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About"
                            )
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

            // Menu items in a scrollable column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Take Order Card
                OutlinedCard(
                    onClick = onNavigateToOrders,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ShoppingCart, "Take Order")
                            Column {
                                Text("Orders")
                                Text(
                                    "Create new customer orders",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }

                // Manage Products Card
                OutlinedCard(
                    onClick = onNavigateToProducts,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Inventory, "Manage Products")
                            Column {
                                Text("Products")
                                Text(
                                    "Add or edit products and prices",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }

                // Manage Customers Card
                OutlinedCard(
                    onClick = onNavigateToCustomers,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.People, "Manage Customers")
                            Column {
                                Text("Customers")
                                Text(
                                    "Add or edit customer information",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }

                // Acquire Produce Card
                OutlinedCard(
                    onClick = onNavigateToAcquire,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddShoppingCart, "Acquire Produce")
                            Column {
                                Text("Inventory")
                                Text(
                                    "Record product acquisitions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }

                // Remittance Card
                OutlinedCard(
                    onClick = onNavigateToRemittance,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Payments, "Remittance")
                            Column {
                                Text("Remittance")
                                Text(
                                    "Record remittance transactions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }

                // Manage Employees Card
                OutlinedCard(
                    onClick = onNavigateToEmployees,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.People, "Manage Employees")
                            Column {
                                Text("Green Crew")
                                Text(
                                    "Add or edit employee information",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }

                // Farm Operations Card
                OutlinedCard(
                    onClick = onNavigateToFarmOps,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Agriculture, "Farm Operations")
                            Column {
                                Text("Farm Operations")
                                Text(
                                    "Record farm activities",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
                    }
                }

                // Export Data Card
                OutlinedCard(
                    onClick = onNavigateToExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FileDownload, "Export Data")
                            Column {
                                Text("Export Data")
                                Text(
                                    "Export data to CSV files",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, "Navigate")
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
            onNavigateToAbout = {}
        )
    }
} 