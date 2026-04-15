package com.redn.farm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.redn.farm.ui.screens.main.MainScreen
import com.redn.farm.ui.screens.manage.products.ManageProductsScreen
import com.redn.farm.ui.screens.manage.customers.ManageCustomersScreen
import com.redn.farm.ui.screens.order.ActiveSrpsScreen
import com.redn.farm.ui.screens.order.OrderCustomerPickerScreen
import com.redn.farm.ui.screens.order.OrderProductPickerScreen
import com.redn.farm.ui.screens.order.TakeOrderScreen
import com.redn.farm.ui.screens.order.TakeOrderViewModel
import com.redn.farm.ui.screens.order.history.OrderHistoryScreen
import com.redn.farm.ui.screens.order.history.EditOrderScreen
import com.redn.farm.ui.screens.order.history.OrderDetailScreen
import com.redn.farm.ui.screens.acquire.AcquireProduceScreen
import com.redn.farm.ui.screens.acquire.AcquireProduceViewModel
import com.redn.farm.ui.screens.acquire.AcquisitionFormScreen
import com.redn.farm.ui.screens.remittance.RemittanceFormScreen
import com.redn.farm.ui.screens.remittance.RemittanceScreen
import com.redn.farm.ui.screens.remittance.RemittanceViewModel
import com.redn.farm.ui.screens.manage.employees.EmployeeFormScreen
import com.redn.farm.ui.screens.manage.employees.ManageEmployeesScreen
import com.redn.farm.ui.screens.manage.employees.ManageEmployeesViewModel
import com.redn.farm.ui.screens.manage.employees.payment.EmployeePaymentScreen
import com.redn.farm.ui.screens.manage.employees.payment.PaymentFormScreen
import com.redn.farm.ui.screens.farmops.FarmOperationFormScreen
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.security.Rbac
import com.redn.farm.ui.screens.eod.DayCloseHistoryScreen
import com.redn.farm.ui.screens.eod.DayCloseScreen
import com.redn.farm.ui.screens.eod.OutstandingInventoryScreen
import com.redn.farm.ui.screens.manage.customers.CustomerFormScreen
import com.redn.farm.ui.screens.manage.customers.ManageCustomersViewModel
import com.redn.farm.ui.screens.manage.products.ProductFormScreen
import com.redn.farm.ui.screens.manage.products.ProductPriceHistoryScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DatabaseMigration : Screen("database_migration")
    object Main : Screen("main")
    object Products : Screen("products")
    object ProductForm : Screen("product_form/{productId}") {
        fun createRoute(productId: String) = "product_form/$productId"
    }
    object ProductPriceHistory : Screen("product_price_history/{productId}") {
        fun createRoute(productId: String) = "product_price_history/$productId"
    }
    object Customers : Screen("customers")
    object CustomerForm : Screen("customer_form/{customerId}") {
        fun createRoute(customerId: String) = "customer_form/$customerId"
    }
    object Orders : Screen("orders")
    object OrderCustomerPicker : Screen("order_customer_picker")
    object OrderProductPicker : Screen("order_product_picker")
    object OrderHistory : Screen("order_history")
    object ActiveSrps : Screen("active_srps")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: Int) = "order_detail/$orderId"
    }
    object EditOrder : Screen("edit_order/{orderId}") {
        fun createRoute(orderId: Int) = "edit_order/$orderId"
    }
    object Acquire : Screen("acquire")
    object AcquisitionForm : Screen("acquisition_form/{acquisitionId}") {
        fun createRoute(acquisitionId: String) = "acquisition_form/$acquisitionId"
    }
    object Remittance : Screen("remittance")
    object RemittanceForm : Screen("remittance_add_edit/{remittanceId}") {
        fun createRoute(remittanceId: String) = "remittance_add_edit/$remittanceId"
    }
    object Employees : Screen("employees")
    object EmployeeForm : Screen("employee_add_edit/{employeeId}") {
        fun createRoute(employeeId: String) = "employee_add_edit/$employeeId"
    }
    object EmployeePayments : Screen("employee_payments/{employeeId}/{employeeName}") {
        fun createRoute(employeeId: Int, employeeName: String) =
            "employee_payments/$employeeId/${employeeName.replace(" ", "_")}"
    }
    object EmployeePaymentForm : Screen("employee_payment_form/{employeeId}/{employeeName}/{paymentId}") {
        fun createRoute(employeeId: Int, employeeName: String, paymentId: Int) =
            "employee_payment_form/$employeeId/${employeeName.replace(" ", "_")}/$paymentId"
    }
    object FarmOps : Screen("farm_ops")
    object FarmOperationForm : Screen("farm_op_form/{operationId}") {
        fun createRoute(operationId: String) = "farm_op_form/$operationId"
    }
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

    // Epic 12 — End of Day Operations
    object DayClose : Screen("day_close/{businessDateMillis}") {
        fun createRoute(businessDateMillis: Long) = "day_close/$businessDateMillis"
        /** Opens today's day close. */
        fun createRouteToday() = "day_close/${System.currentTimeMillis()}"
    }
    object DayCloseHistory : Screen("day_close_history")
    object OutstandingInventory : Screen("outstanding_inventory")
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
                onNavigateToDayClose = {
                    navController.navigate(Screen.DayClose.createRouteToday())
                },
            )
        }
        composable(Screen.Products.route) {
            RequireRole(navController, Rbac.ROLES_PRODUCTS) {
                ManageProductsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToProductForm = { productId ->
                        navController.navigate(Screen.ProductForm.createRoute(productId))
                    },
                    onNavigateToPriceHistory = { productId ->
                        navController.navigate(Screen.ProductPriceHistory.createRoute(productId))
                    },
                )
            }
        }
        composable(
            route = Screen.ProductForm.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { entry ->
            val pid = entry.arguments?.getString("productId") ?: return@composable
            val productsListEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Products.route)
            }
            RequireRole(navController, Rbac.ROLES_PRODUCTS) {
                ProductFormScreen(
                    productId = pid,
                    onNavigateBack = { navController.navigateUp() },
                    onOpenPresetDetail = { presetId ->
                        navController.navigate(Screen.PresetDetail.createRoute(presetId))
                    },
                    onNavigateToPriceHistory = {
                        navController.navigate(Screen.ProductPriceHistory.createRoute(pid))
                    },
                    viewModel = hiltViewModel(productsListEntry)
                )
            }
        }
        composable(
            route = Screen.ProductPriceHistory.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { entry ->
            val historyPid = entry.arguments?.getString("productId") ?: return@composable
            val productsListEntryForHistory = remember(entry) {
                navController.getBackStackEntry(Screen.Products.route)
            }
            RequireRole(navController, Rbac.ROLES_PRODUCTS) {
                ProductPriceHistoryScreen(
                    productId = historyPid,
                    onNavigateBack = { navController.navigateUp() },
                    onOpenPresetDetail = { presetId ->
                        navController.navigate(Screen.PresetDetail.createRoute(presetId))
                    },
                    viewModel = hiltViewModel(productsListEntryForHistory)
                )
            }
        }
        composable(Screen.Customers.route) {
            RequireRole(navController, Rbac.ROLES_CUSTOMERS) {
                ManageCustomersScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCustomerForm = { customerId ->
                        navController.navigate(Screen.CustomerForm.createRoute(customerId))
                    }
                )
            }
        }
        composable(
            route = Screen.CustomerForm.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType }
            )
        ) { entry ->
            val cid = entry.arguments?.getString("customerId") ?: return@composable
            val customersListEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Customers.route)
            }
            RequireRole(navController, Rbac.ROLES_CUSTOMERS) {
                CustomerFormScreen(
                    customerId = cid,
                    onNavigateBack = { navController.navigateUp() },
                    viewModel = hiltViewModel(customersListEntry)
                )
            }
        }
        composable(Screen.Orders.route) { entry ->
            val ordersEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Orders.route)
            }
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
                    },
                    onNavigateToCustomerPicker = {
                        navController.navigate(Screen.OrderCustomerPicker.route)
                    },
                    onNavigateToProductPicker = {
                        navController.navigate(Screen.OrderProductPicker.route)
                    },
                    viewModel = hiltViewModel(ordersEntry)
                )
            }
        }
        composable(Screen.OrderCustomerPicker.route) { entry ->
            val ordersEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Orders.route)
            }
            RequireRole(navController, Rbac.ROLES_ORDERS_FLOW) {
                val takeOrderVm = hiltViewModel<TakeOrderViewModel>(ordersEntry)
                OrderCustomerPickerScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onCustomerSelected = { customer ->
                        takeOrderVm.selectCustomer(customer)
                        navController.navigateUp()
                    },
                    viewModel = takeOrderVm,
                )
            }
        }
        composable(Screen.OrderProductPicker.route) { entry ->
            val ordersEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Orders.route)
            }
            RequireRole(navController, Rbac.ROLES_ORDERS_FLOW) {
                OrderProductPickerScreen(
                    onNavigateBack = { navController.navigateUp() },
                    viewModel = hiltViewModel(ordersEntry),
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
        composable(Screen.OrderHistory.route) { entry ->
            val orderHistoryListEntry = remember(entry) {
                navController.getBackStackEntry(Screen.OrderHistory.route)
            }
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
                    },
                    viewModel = hiltViewModel(orderHistoryListEntry)
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
            val orderHistoryListEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.OrderHistory.route)
            }
            RequireRole(navController, Rbac.ROLES_ORDERS_FLOW) {
                OrderDetailScreen(
                    orderId = orderId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = {
                        navController.navigate(Screen.EditOrder.createRoute(orderId))
                    },
                    viewModel = hiltViewModel(orderHistoryListEntry)
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
            val orderHistoryListEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.OrderHistory.route)
            }
            RequireRole(navController, Rbac.ROLES_ORDERS_FLOW) {
                EditOrderScreen(
                    orderId = orderId,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = hiltViewModel(orderHistoryListEntry)
                )
            }
        }
        composable(Screen.Acquire.route) {
            RequireRole(navController, Rbac.ROLES_ACQUIRE) {
                AcquireProduceScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAcquisitionForm = { acquisitionId ->
                        navController.navigate(Screen.AcquisitionForm.createRoute(acquisitionId))
                    }
                )
            }
        }
        composable(
            route = Screen.AcquisitionForm.route,
            arguments = listOf(
                navArgument("acquisitionId") { type = NavType.StringType }
            )
        ) { entry ->
            val acquisitionIdKey = entry.arguments?.getString("acquisitionId") ?: return@composable
            val acquireListEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Acquire.route)
            }
            RequireRole(navController, Rbac.ROLES_ACQUIRE) {
                val acquireVm = hiltViewModel<AcquireProduceViewModel>(acquireListEntry)
                val canViewPresetDetail by acquireVm.canViewPresetDetail.collectAsState()
                AcquisitionFormScreen(
                    acquisitionIdKey = acquisitionIdKey,
                    onNavigateBack = { navController.navigateUp() },
                    canViewPresetDetail = canViewPresetDetail,
                    onOpenPresetDetail = { presetId ->
                        navController.navigate(Screen.PresetDetail.createRoute(presetId))
                    },
                    viewModel = acquireVm,
                )
            }
        }
        composable(Screen.Remittance.route) {
            RequireRole(navController, Rbac.ROLES_REMITTANCE_HUB) {
                RemittanceScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToForm = { remittanceId ->
                        navController.navigate(Screen.RemittanceForm.createRoute(remittanceId))
                    }
                )
            }
        }
        composable(
            route = Screen.RemittanceForm.route,
            arguments = listOf(
                navArgument("remittanceId") { type = NavType.StringType }
            )
        ) { entry ->
            val remittanceIdKey = entry.arguments?.getString("remittanceId") ?: return@composable
            val remittanceListEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Remittance.route)
            }
            RequireRole(navController, Rbac.ROLES_REMITTANCE_HUB) {
                RemittanceFormScreen(
                    remittanceIdKey = remittanceIdKey,
                    onNavigateBack = { navController.navigateUp() },
                    viewModel = hiltViewModel(remittanceListEntry)
                )
            }
        }
        composable(Screen.Employees.route) {
            RequireRole(navController, Rbac.ROLES_EMPLOYEES) {
                ManageEmployeesScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToPayments = { employeeId, employeeName ->
                        navController.navigate(Screen.EmployeePayments.createRoute(employeeId, employeeName))
                    },
                    onNavigateToEmployeeForm = { employeeId ->
                        navController.navigate(Screen.EmployeeForm.createRoute(employeeId))
                    }
                )
            }
        }
        composable(
            route = Screen.EmployeeForm.route,
            arguments = listOf(
                navArgument("employeeId") { type = NavType.StringType }
            )
        ) { entry ->
            val employeeIdKey = entry.arguments?.getString("employeeId") ?: return@composable
            val employeesListEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Employees.route)
            }
            RequireRole(navController, Rbac.ROLES_EMPLOYEES) {
                EmployeeFormScreen(
                    employeeIdKey = employeeIdKey,
                    onNavigateBack = { navController.navigateUp() },
                    viewModel = hiltViewModel(employeesListEntry)
                )
            }
        }
        composable(
            route = Screen.EmployeePayments.route,
            arguments = listOf(
                navArgument("employeeId") { type = NavType.IntType },
                navArgument("employeeName") { type = NavType.StringType }
            )
        ) { entry ->
            val employeeId = entry.arguments?.getInt("employeeId") ?: return@composable
            val employeeName = entry.arguments?.getString("employeeName")?.replace("_", " ") ?: return@composable
            val employeePaymentsListEntry = remember(entry) {
                navController.getBackStackEntry(Screen.EmployeePayments.route)
            }
            RequireRole(navController, Rbac.ROLES_EMPLOYEES) {
                EmployeePaymentScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToPaymentForm = { pid ->
                        navController.navigate(
                            Screen.EmployeePaymentForm.createRoute(employeeId, employeeName, pid)
                        )
                    },
                    employeeId = employeeId,
                    employeeName = employeeName,
                    viewModel = hiltViewModel(employeePaymentsListEntry)
                )
            }
        }
        composable(
            route = Screen.EmployeePaymentForm.route,
            arguments = listOf(
                navArgument("employeeId") { type = NavType.IntType },
                navArgument("employeeName") { type = NavType.StringType },
                navArgument("paymentId") { type = NavType.IntType }
            )
        ) { entry ->
            val formEmployeeId = entry.arguments?.getInt("employeeId") ?: return@composable
            val formEmployeeName = entry.arguments?.getString("employeeName")?.replace("_", " ")
                ?: return@composable
            val formPaymentId = entry.arguments?.getInt("paymentId") ?: return@composable
            val employeePaymentsListEntry = remember(entry) {
                navController.getBackStackEntry(Screen.EmployeePayments.route)
            }
            RequireRole(navController, Rbac.ROLES_EMPLOYEES) {
                PaymentFormScreen(
                    employeeId = formEmployeeId,
                    employeeName = formEmployeeName,
                    paymentId = formPaymentId,
                    onNavigateBack = { navController.navigateUp() },
                    viewModel = hiltViewModel(employeePaymentsListEntry)
                )
            }
        }
        composable(Screen.FarmOps.route) {
            RequireRole(navController, Rbac.ROLES_FARM_OPS) {
                FarmOperationsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToOperationForm = { operationId ->
                        navController.navigate(Screen.FarmOperationForm.createRoute(operationId))
                    }
                )
            }
        }
        composable(
            route = Screen.FarmOperationForm.route,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType }
            )
        ) { entry ->
            val oid = entry.arguments?.getString("operationId") ?: return@composable
            // Share VM with farm_ops list when that route is on the stack; else use form entry (e.g. deep link to history + edit).
            val farmOpsVmOwner = remember(entry) {
                try {
                    navController.getBackStackEntry(Screen.FarmOps.route)
                } catch (_: IllegalArgumentException) {
                    entry
                }
            }
            RequireRole(navController, Rbac.ROLES_FARM_OPS) {
                FarmOperationFormScreen(
                    operationIdKey = oid,
                    onNavigateBack = { navController.navigateUp() },
                    viewModel = hiltViewModel(farmOpsVmOwner)
                )
            }
        }
        composable(Screen.FarmOpsHistory.route) {
            RequireRole(navController, Rbac.ROLES_FARM_OPS) {
                FarmOperationHistoryScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToOperationForm = { operationId ->
                        navController.navigate(Screen.FarmOperationForm.createRoute(operationId))
                    }
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
                    onNavigateBack = { navController.navigateUp() },
                    onSaveSuccessNavigateToPresetHistory = {
                        navController.navigate(Screen.PresetHistory.route) {
                            popUpTo(Screen.PricingPresetsHome.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
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

        // ── Epic 12 — End of Day Operations ───────────────────────────────────
        composable(
            route = Screen.DayClose.route,
            arguments = listOf(
                navArgument("businessDateMillis") { type = NavType.LongType }
            )
        ) { entry ->
            val businessDateMillis = entry.arguments?.getLong("businessDateMillis")
                ?: System.currentTimeMillis()
            RequireRole(navController, Rbac.ROLES_DAY_CLOSE) {
                val ctx = LocalContext.current.applicationContext
                val session = remember(ctx) { SessionManager(ctx) }
                DayCloseScreen(
                    businessDateMillis = businessDateMillis,
                    username = session.getUsername() ?: "unknown",
                    role = Rbac.normalizeRole(session.getRole()),
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToHistory = { navController.navigate(Screen.DayCloseHistory.route) },
                    onNavigateToOutstandingInventory = {
                        navController.navigate(Screen.OutstandingInventory.route)
                    },
                    onNavigateToOrderDetail = { orderId ->
                        navController.navigate(Screen.OrderDetail.createRoute(orderId))
                    },
                )
            }
        }
        composable(Screen.DayCloseHistory.route) {
            RequireRole(navController, Rbac.ROLES_DAY_CLOSE_HISTORY) {
                DayCloseHistoryScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onOpenClose = { dateMillis ->
                        navController.navigate(Screen.DayClose.createRoute(dateMillis))
                    },
                )
            }
        }
        composable(Screen.OutstandingInventory.route) {
            RequireRole(navController, Rbac.ROLES_OUTSTANDING_INVENTORY) {
                OutstandingInventoryScreen(
                    onNavigateBack = { navController.navigateUp() },
                )
            }
        }
    }
}