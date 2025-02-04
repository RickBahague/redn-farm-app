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
import com.redn.farm.ui.screens.order.TakeOrderScreen
import com.redn.farm.ui.screens.order.history.OrderHistoryScreen
import com.redn.farm.ui.screens.order.history.EditOrderScreen
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
import androidx.compose.ui.Modifier

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DatabaseMigration : Screen("database_migration")
    object Main : Screen("main")
    object Products : Screen("products")
    object Customers : Screen("customers")
    object Orders : Screen("orders")
    object OrderHistory : Screen("order_history")
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
                    navController.navigate(Screen.DatabaseMigration.route) {
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
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Products.route) {
            ManageProductsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Customers.route) {
            ManageCustomersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Orders.route) {
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
                }
            )
        }
        composable(Screen.OrderHistory.route) {
            OrderHistoryScreen(
                onNavigateBack = { 
                    navController.navigate(Screen.Orders.route) {
                        popUpTo(Screen.Orders.route) {
                            inclusive = false
                        }
                    }
                },
                onNavigateToEdit = { orderId ->
                    navController.navigate(Screen.EditOrder.createRoute(orderId))
                },
                onNavigateToView = { orderId ->
                    navController.navigate(Screen.EditOrder.createRoute(orderId))
                }
            )
        }
        composable(
            route = Screen.EditOrder.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            EditOrderScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Acquire.route) {
            AcquireProduceScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Remittance.route) {
            RemittanceScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Employees.route) {
            ManageEmployeesScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToPayments = { employeeId, employeeName ->
                    navController.navigate(Screen.EmployeePayments.createRoute(employeeId, employeeName))
                }
            )
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
            
            EmployeePaymentScreen(
                onNavigateBack = { navController.navigateUp() },
                employeeId = employeeId,
                employeeName = employeeName
            )
        }
        composable(Screen.FarmOps.route) {
            FarmOperationsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.FarmOpsHistory.route) {
            FarmOperationHistoryScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Export.route) {
            ExportScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
} 