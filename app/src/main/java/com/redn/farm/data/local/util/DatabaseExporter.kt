package com.redn.farm.data.local.util

import android.content.Context
import android.provider.Settings
import com.google.gson.GsonBuilder
import com.redn.farm.data.local.FarmDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DatabaseExporter(
    private val context: Context,
    private val database: FarmDatabase
) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val machineId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    suspend fun exportDatabase(): String = withContext(Dispatchers.IO) {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        val exportData = mutableMapOf<String, List<Map<String, Any?>>>()

        // Export Products
        val products = database.productDao().getAllProducts().first()
        exportData["products"] = products.map { it.toMap().plus("machine_id" to machineId) }

        // Export Product Prices
        val productPrices = database.productPriceDao().getAllProductPrices().first()
        exportData["product_prices"] = productPrices.map { it.toMap().plus("machine_id" to machineId) }

        // Export Customers
        val customers = database.customerDao().getAllCustomers().first()
        exportData["customers"] = customers.map { it.toMap().plus("machine_id" to machineId) }

        // Export Orders and Order Items
        val orders = database.orderDao().getAllOrders().first()
        exportData["orders"] = orders.map { it.order.toMap().plus("machine_id" to machineId) }

        // Export Acquisitions
        val acquisitions = database.acquisitionDao().getAllAcquisitions().first()
        exportData["acquisitions"] = acquisitions.map { it.toMap().plus("machine_id" to machineId) }

        // Export Remittances
        val remittances = database.remittanceDao().getAllRemittances().first()
        exportData["remittances"] = remittances.map { it.toMap().plus("machine_id" to machineId) }

        // Export Employees
        val employees = database.employeeDao().getAllEmployees().first()
        exportData["employees"] = employees.map { it.toMap().plus("machine_id" to machineId) }

        // Export Employee Payments
        val employeePayments = database.employeePaymentDao().getAllPayments().first()
        exportData["employee_payments"] = employeePayments.map { it.payment.toMap().plus("machine_id" to machineId) }

        // Export Farm Operations
        val farmOperations = database.farmOperationDao().getAllOperations().first()
        exportData["farm_operations"] = farmOperations.map { it.toMap().plus("machine_id" to machineId) }

        // Create export directory if it doesn't exist
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        // Write to file
        val exportFile = File(exportDir, "farm_db_export_${timestamp}.json")
        exportFile.writeText(gson.toJson(exportData))

        return@withContext exportFile.absolutePath
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any.toMap(): Map<String, Any?> {
        return gson.fromJson(gson.toJson(this), Map::class.java) as Map<String, Any?>
    }
} 