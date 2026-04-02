package com.redn.farm.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.redn.farm.ui.screens.main.MainScreen
import com.redn.farm.ui.screens.manage.products.ManageProductsScreen
import com.redn.farm.ui.screens.manage.customers.ManageCustomersScreen
import com.redn.farm.ui.screens.order.ActiveSrpsScreen
import com.redn.farm.ui.screens.order.TakeOrderScreen
import com.redn.farm.ui.screens.order.history.OrderHistoryScreen
import com.redn.farm.ui.screens.order.history.EditOrderScreen
import com.redn.farm.ui.screens.order.history.OrderDetailScreen
import com.redn.farm.ui.screens.acquire.AcquireProduceScreen
import com.redn.farm.ui.screens.remittance.RemittanceScreen
import com.redn.farm.ui.screens.manage.employees.ManageEmployeesScreen
import com.redn.farm.ui.screens.manage.employees.payment.EmployeePaymentScreen
import com.redn.farm.ui.screens.farmops.FarmOperationsScreen
import com.redn.farm.ui.screens.farmops.history.FarmOperationHistoryScreen
import com.redn.farm.ui.screens.export.ExportScreen
import com.redn.farm.ui.screens.about.AboutScreen
import com.redn.farm.ui.screens.database.DatabaseMigrationScreen
import com.redn.farm.ui.screens.login.LoginScreen
import com.redn.farm.ui.screens.pricing.PresetActivationPreviewScreen
import com.redn.farm.ui.screens.pricing.PresetDetailScreen
import com.redn.farm.ui.screens.pricing.PresetHistoryScreen
import com.redn.farm.ui.screens.pricing.PricingPresetEditorScreen
import com.redn.farm.ui.screens.pricing.PricingPresetsHomeScreen
import com.redn.farm.ui.screens.settings.SettingsScreen
import com.redn.farm.ui.screens.profile.ProfileScreen
import com.redn.farm.ui.screens.profile.ChangePasswordScreen
import com.redn.farm.ui.screens.profile.UserManagementScreen
import androidx.compose.ui.Modifier
import com.redn.farm.security.Rbac

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DatabaseMigration : Screen("database_migration")
    object Main : Screen("main")
    object Products : Screen("products")
    object Customers : Screen("customers")
    object Orders : Screen("orders")
    object OrderHistory : Screen("order_history")
    object ActiveSrps : Screen("active_srps")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: Int) = "order_detail/$orderId"
    }
    object EditOrder : Screen("edit_order/{orderId}") {
        fun createRoute(orderId: Int) = "edit_order/$orderId"
    }
    object Acquire : Screen("acquire")
    object Remittance : Screen("remittance")
    object Employees : Screen("employees")
    object EmployeePayments : Screen("employee_payments/{employeeId}/{employeeName}") {
        fun createRoute(employeeId: Int, employeeName: String) = 
            "employee_payments/$employeeId/${employeeName.replace(" ", "_")}"
    }
    object FarmOps : Screen("farm_ops")
    object FarmOpsHistory : Screen("farm_ops_history")
    object Export : Screen("export")
    object About : Screen("about")
    object Profile : Screen("profile")
    object ChangePassword : Screen("change_password")
    object UserManagement : Screen("user_management")
    object Settings : Screen("settings")
    object PricingPresetsHome : Screen("pricing_presets")
    object PresetHistory : Screen("preset_history")
    object PricingPresetEditor : Screen("pricing_preset_editor/{sourcePresetId}") {
        fun createRoute(sourcePresetId: String) = "pricing_preset_editor/$sourcePresetId"
    }
    object PresetDetail : Screen("preset_detail/{presetId}") {
        fun createRoute(presetId: String) = "preset_detail/$presetId"
    }
    object PresetActivationPreview : Screen("preset_activation_preview/{presetId}") {
        fun createRoute(presetId: String) = "preset_activation_preview/$presetId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DatabaseMigration.route) {
            DatabaseMigrationScreen(
                onDatabaseReady = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.DatabaseMigration.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToOrders = {
                    navController.navigate(Screen.Orders.route)
                },
                onNavigateToProducts = {
                    navController.navigate(Screen.Products.route)
                },
                onNavigateToCustomers = {
                    navController.navigate(Screen.Customers.route)
                },
                onNavigateToAcquire = {
                    navController.navigate(Screen.Acquire.route)
                },
                onNavigateToRemittance = {
                    navController.navigate(Screen.Remittance.route)
                },
                onNavigateToEmployees = {
                    navController.navigate(Screen.Employees.route)
                },
                onNavigateToFarmOps = {
                    navController.navigate(Screen.FarmOps.route)
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Products.route) {
            RequireRole(navController, Rbac.ROLES_PRODUCTS) {
                ManageProductsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
        composable(Screen.Customers.route) {
            RequireRole(navController, Rbac.ROLES_CUSTOMERS) {
                ManageCustomersScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.Orders.route) {
            RequireRole(navController, Rbac.ROLES_ORDERS_FLOW) {
                TakeOrderScreen(
                    onNavigateBack = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Main.route) {
                                inclusive = false
                            }
                        }
                    },
                    onNavigateToOrderHistory = {
                        navController.navigate(Screen.OrderHistory.route)
                    },
                    onNavigateToActiveSrps = {
                        navController.navigate(Screen.ActiveSrps.route)
                    }
                )
            }
        }
        composable(Screen.ActiveSrps.route) {
            RequireRole(navController, Rbac.ROLES_ACTIVE_SRPS) {
                ActiveSrpsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.OrderHistory.route) {
            RequireRole(navController, Rbac.ROLES_ORDERS_FLOW) {
                OrderHistoryScreen(
                    onNavigateBack = {
                        navController.navigate(Screen.Orders.route) {
                            popUpTo(Screen.Orders.route) {
                                inclusive = false
                            }
                        }
                    },
                    onNavigateToOrderDetail = { orderId ->
                        navController.navigate(Screen.OrderDetail.createRoute(orderId))
                    }
                )
            }
        }
        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            RequireRole(navController, Rbac.ROLES_ORDERS_FLOW) {
                OrderDetailScreen(
                    orderId = orderId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = {
                        navController.navigate(Screen.EditOrder.createRoute(orderId))
                    }
                )
            }
        }
        composable(
            route = Screen.EditOrder.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            RequireRole(navController, Rbac.ROLES_ORDERS_FLOW) {
                EditOrderScreen(
                    orderId = orderId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.Acquire.route) {
            RequireRole(navController, Rbac.ROLES_ACQUIRE) {
                AcquireProduceScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
        composable(Screen.Remittance.route) {
            RequireRole(navController, Rbac.ROLES_REMITTANCE) {
                RemittanceScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
        composable(Screen.Employees.route) {
            RequireRole(navController, Rbac.ROLES_EMPLOYEES) {
                ManageEmployeesScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToPayments = { employeeId, employeeName ->
                        navController.navigate(Screen.EmployeePayments.createRoute(employeeId, employeeName))
                    }
                )
            }
        }
        composable(
            route = Screen.EmployeePayments.route,
            arguments = listOf(
                navArgument("employeeId") { type = NavType.IntType },
                navArgument("employeeName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val employeeId = backStackEntry.arguments?.getInt("employeeId") ?: return@composable
            val employeeName = backStackEntry.arguments?.getString("employeeName")?.replace("_", " ") ?: return@composable

            RequireRole(navController, Rbac.ROLES_EMPLOYEES) {
                EmployeePaymentScreen(
                    onNavigateBack = { navController.navigateUp() },
                    employeeId = employeeId,
                    employeeName = employeeName
                )
            }
        }
        composable(Screen.FarmOps.route) {
            RequireRole(navController, Rbac.ROLES_FARM_OPS) {
                FarmOperationsScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
        composable(Screen.FarmOpsHistory.route) {
            RequireRole(navController, Rbac.ROLES_FARM_OPS) {
                FarmOperationHistoryScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
        composable(Screen.Export.route) {
            RequireRole(navController, Rbac.ROLES_EXPORT) {
                ExportScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onNavigateToUserManagement = {
                    navController.navigate(Screen.UserManagement.route)
                }
            )
        }
        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.UserManagement.route) {
            RequireRole(navController, Rbac.ROLES_USER_MANAGEMENT) {
                UserManagementScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
        composable(Screen.Settings.route) {
            RequireRole(navController, Rbac.ROLES_SETTINGS_AND_PRICING) {
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToPricingPresets = {
                        navController.navigate(Screen.PricingPresetsHome.route)
                    }
                )
            }
        }
        composable(Screen.PricingPresetsHome.route) {
            RequireRole(navController, Rbac.ROLES_SETTINGS_AND_PRICING) {
                PricingPresetsHomeScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNewPreset = {
                        navController.navigate(Screen.PricingPresetEditor.createRoute("new"))
                    },
                    onPresetHistory = { navController.navigate(Screen.PresetHistory.route) }
                )
            }
        }
        composable(Screen.PresetHistory.route) {
            RequireRole(navController, Rbac.ROLES_SETTINGS_AND_PRICING) {
                PresetHistoryScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onPresetClick = { id ->
                        navController.navigate(Screen.PresetDetail.createRoute(id))
                    }
                )
            }
        }
        composable(
            route = Screen.PricingPresetEditor.route,
            arguments = listOf(
                navArgument("sourcePresetId") { type = NavType.StringType }
            )
        ) {
            RequireRole(navController, Rbac.ROLES_SETTINGS_AND_PRICING) {
                PricingPresetEditorScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
        composable(
            route = Screen.PresetDetail.route,
            arguments = listOf(
                navArgument("presetId") { type = NavType.StringType }
            )
        ) {
            RequireRole(navController, Rbac.ROLES_SETTINGS_AND_PRICING) {
                PresetDetailScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onRestoreToEditor = { pid ->
                        navController.navigate(Screen.PricingPresetEditor.createRoute(pid))
                    },
                    onActivatePreview = { pid ->
                        navController.navigate(Screen.PresetActivationPreview.createRoute(pid))
                    }
                )
            }
        }
        composable(
            route = Screen.PresetActivationPreview.route,
            arguments = listOf(
                navArgument("presetId") { type = NavType.StringType }
            )
        ) {
            RequireRole(navController, Rbac.ROLES_SETTINGS_AND_PRICING) {
                PresetActivationPreviewScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onActivationComplete = {
                        navController.popBackStack(
                            Screen.PricingPresetsHome.route,
                            inclusive = false
                        )
                    }
                )
            }
        }
    }
} 