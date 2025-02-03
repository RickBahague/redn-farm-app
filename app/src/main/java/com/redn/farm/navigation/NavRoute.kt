sealed class NavRoute(val route: String) {
    object Main : NavRoute("main")
    object TakeOrder : NavRoute("take_order")
    object OrderHistory : NavRoute("order_history")
    object ManageProducts : NavRoute("manage_products")
    object ManageCustomers : NavRoute("manage_customers")
    object AcquireProduce : NavRoute("acquire_produce")
    object Remittance : NavRoute("remittance")
    object ManageEmployees : NavRoute("manage_employees")
    object FarmOperations : NavRoute("farm_operations")
    object Export : NavRoute("export")
} 