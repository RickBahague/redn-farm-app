package com.redn.farm.data.export

import android.content.Context
import com.redn.farm.data.local.entity.UserEntity
import com.redn.farm.data.model.Order
import com.redn.farm.data.model.OrderItem
import com.redn.farm.data.model.Customer
import com.redn.farm.data.model.EmployeePayment
import com.redn.farm.data.model.Employee
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.data.model.ProductPrice
import com.redn.farm.data.model.Product
import com.redn.farm.data.model.Remittance
import com.redn.farm.data.model.Acquisition
import com.redn.farm.utils.DeviceUtils
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CsvExportService(private val context: Context) {

    private fun getExportsDirectory(): File {
        val exportsDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }
        return exportsDir
    }

    private fun makeTimestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    /** Double-quote CSV text field; escape internal quotes. */
    private fun csvTextField(value: String?): String {
        val v = value ?: ""
        return "\"${v.replace("\"", "\"\"")}\""
    }

    private fun fileFor(prefix: String, sharedTimestamp: String?): File {
        val ts = sharedTimestamp ?: makeTimestamp()
        return File(getExportsDirectory(), "${prefix}_$ts.csv")
    }

    /**
     * Security: password hash is **not** exported (AUTH).
     */
    fun exportUsers(users: List<UserEntity>, sharedTimestamp: String? = null): File {
        val file = fileFor("users", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("UserId,Username,FullName,Role,IsActive,DateCreated,DateUpdated,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            users.forEach { u ->
                writer.append("${u.user_id},")
                    .append(csvTextField(u.username)).append(",")
                    .append(csvTextField(u.full_name)).append(",")
                    .append(csvTextField(u.role)).append(",")
                    .append("${u.is_active},")
                    .append("${u.date_created},")
                    .append("${u.date_updated},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportCustomers(customers: List<Customer>, sharedTimestamp: String? = null): File {
        val file = fileFor("customers", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("CustomerId,FirstName,LastName,Contact,CustomerType,Address,City,Province,PostalCode,DateCreated,DateUpdated,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            customers.forEach { customer ->
                writer.append("${customer.customer_id},")
                    .append(csvTextField(customer.firstname)).append(",")
                    .append(csvTextField(customer.lastname)).append(",")
                    .append(csvTextField(customer.contact)).append(",")
                    .append(csvTextField(customer.customer_type.name)).append(",")
                    .append(csvTextField(customer.address)).append(",")
                    .append(csvTextField(customer.city)).append(",")
                    .append(csvTextField(customer.province)).append(",")
                    .append(csvTextField(customer.postal_code)).append(",")
                    .append("${customer.date_created},")
                    .append("${customer.date_updated},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportOrders(orders: List<Order>, sharedTimestamp: String? = null): File {
        val file = fileFor("orders", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("OrderId,CustomerId,Channel,CustomerName,CustomerContact,TotalAmount,OrderDate,UpdateDate,IsPaid,IsDelivered,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            orders.forEach { order ->
                writer.append("${order.order_id},")
                    .append("${order.customer_id},")
                    .append(csvTextField(order.channel)).append(",")
                    .append(csvTextField(order.customerName)).append(",")
                    .append(csvTextField(order.customerContact)).append(",")
                    .append("${order.total_amount},")
                    .append("${order.order_date},")
                    .append("${order.order_update_date},")
                    .append("${order.is_paid},")
                    .append("${order.is_delivered},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportOrderItems(orderItems: List<OrderItem>, sharedTimestamp: String? = null): File {
        val file = fileFor("order_items", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("Id,OrderId,ProductId,ProductName,Quantity,PricePerUnit,IsPerKg,TotalPrice,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            orderItems.forEach { item ->
                writer.append("${item.id},")
                    .append("${item.order_id},")
                    .append(csvTextField(item.product_id)).append(",")
                    .append(csvTextField(item.product_name)).append(",")
                    .append("${item.quantity},")
                    .append("${item.price_per_unit},")
                    .append("${item.is_per_kg},")
                    .append("${item.total_price},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportEmployeePayments(payments: List<EmployeePayment>, sharedTimestamp: String? = null): File {
        val file = fileFor("employee_payments", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("PaymentId,EmployeeId,EmployeeName,Amount,CashAdvanceAmount,LiquidatedAmount,DatePaid,ReceivedDate,Signature,IsFinalized,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            payments.forEach { payment ->
                writer.append("${payment.payment_id},")
                    .append("${payment.employee_id},")
                    .append(csvTextField(payment.employeeName)).append(",")
                    .append("${payment.amount},")
                    .append("${payment.cash_advance_amount ?: ""},")
                    .append("${payment.liquidated_amount ?: ""},")
                    .append("${payment.date_paid},")
                    .append("${payment.received_date ?: ""},")
                    .append(csvTextField(payment.signature)).append(",")
                    .append("${payment.is_finalized},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportEmployees(employees: List<Employee>, sharedTimestamp: String? = null): File {
        val file = fileFor("employees", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("EmployeeId,FirstName,LastName,Contact,DateCreated,DateUpdated,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            employees.forEach { employee ->
                writer.append("${employee.employee_id},")
                    .append(csvTextField(employee.firstname)).append(",")
                    .append(csvTextField(employee.lastname)).append(",")
                    .append(csvTextField(employee.contact)).append(",")
                    .append("${employee.date_created},")
                    .append("${employee.date_updated},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportFarmOperations(operations: List<FarmOperation>, sharedTimestamp: String? = null): File {
        val file = fileFor("farm_operations", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("OperationId,OperationType,OperationDate,Details,Area,WeatherCondition,Personnel,ProductId,ProductName,DateCreated,DateUpdated,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            operations.forEach { operation ->
                writer.append("${operation.operation_id},")
                    .append(csvTextField(operation.operation_type.toString())).append(",")
                    .append("${operation.operation_date},")
                    .append(csvTextField(operation.details)).append(",")
                    .append(csvTextField(operation.area)).append(",")
                    .append(csvTextField(operation.weather_condition)).append(",")
                    .append(csvTextField(operation.personnel)).append(",")
                    .append(csvTextField(operation.product_id)).append(",")
                    .append(csvTextField(operation.product_name)).append(",")
                    .append("${operation.date_created},")
                    .append("${operation.date_updated},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportProductPrices(prices: List<ProductPrice>, sharedTimestamp: String? = null): File {
        val file = fileFor("product_prices", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("PriceId,ProductId,PerKgPrice,PerPiecePrice,DiscountedPerKgPrice,DiscountedPerPiecePrice,DateCreated,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            prices.forEach { price ->
                writer.append("${price.price_id},")
                    .append(csvTextField(price.product_id)).append(",")
                    .append("${price.per_kg_price ?: ""},")
                    .append("${price.per_piece_price ?: ""},")
                    .append("${price.discounted_per_kg_price ?: ""},")
                    .append("${price.discounted_per_piece_price ?: ""},")
                    .append("${price.date_created},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportProducts(products: List<Product>, sharedTimestamp: String? = null): File {
        val file = fileFor("products", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("ProductId,ProductName,ProductDescription,UnitType,IsActive,Category,DefaultPieceCount,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            products.forEach { product ->
                writer.append(csvTextField(product.product_id)).append(",")
                    .append(csvTextField(product.product_name)).append(",")
                    .append(csvTextField(product.product_description)).append(",")
                    .append(csvTextField(product.unit_type)).append(",")
                    .append("${product.is_active},")
                    .append(csvTextField(product.category)).append(",")
                    .append("${product.defaultPieceCount ?: ""},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportRemittances(remittances: List<Remittance>, sharedTimestamp: String? = null): File {
        val file = fileFor("remittances", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append("RemittanceId,Amount,Date,Remarks,DateUpdated,DeviceId\n")
            val deviceId = DeviceUtils.getDeviceId(context)
            remittances.forEach { remittance ->
                writer.append("${remittance.remittance_id},")
                    .append("${remittance.amount},")
                    .append("${remittance.date},")
                    .append(csvTextField(remittance.remarks)).append(",")
                    .append("${remittance.date_updated},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }

    fun exportAcquisitions(acquisitions: List<Acquisition>, sharedTimestamp: String? = null): File {
        val file = fileFor("acquisitions", sharedTimestamp)
        FileWriter(file).use { writer ->
            writer.append(
                "AcquisitionId,ProductId,ProductName,Quantity,PricePerUnit,TotalAmount,IsPerKg,PieceCount," +
                    "DateAcquired,CreatedAt,Location," +
                    "PresetRef,SpoilageRate,AdditionalCostPerKg,HaulingWeightKg,HaulingFeesJson,ChannelsSnapshotJson," +
                    "SrpOnlinePerKg,SrpResellerPerKg,SrpOfflinePerKg," +
                    "SrpOnline500g,SrpOnline250g,SrpOnline100g," +
                    "SrpReseller500g,SrpReseller250g,SrpReseller100g," +
                    "SrpOffline500g,SrpOffline250g,SrpOffline100g," +
                    "SrpOnlinePerPiece,SrpResellerPerPiece,SrpOfflinePerPiece," +
                    "DeviceId\n"
            )
            val deviceId = DeviceUtils.getDeviceId(context)
            acquisitions.forEach { a ->
                writer.append("${a.acquisition_id},")
                    .append(csvTextField(a.product_id)).append(",")
                    .append(csvTextField(a.product_name)).append(",")
                    .append("${a.quantity},")
                    .append("${a.price_per_unit},")
                    .append("${a.total_amount},")
                    .append("${a.is_per_kg},")
                    .append("${a.piece_count ?: ""},")
                    .append("${a.date_acquired},")
                    .append("${a.created_at},")
                    .append(csvTextField(a.location.name)).append(",")
                    .append(csvTextField(a.preset_ref)).append(",")
                    .append("${a.spoilage_rate ?: ""},")
                    .append("${a.additional_cost_per_kg ?: ""},")
                    .append("${a.hauling_weight_kg ?: ""},")
                    .append(csvTextField(a.hauling_fees_json)).append(",")
                    .append(csvTextField(a.channels_snapshot_json)).append(",")
                    .append("${a.srp_online_per_kg ?: ""},")
                    .append("${a.srp_reseller_per_kg ?: ""},")
                    .append("${a.srp_offline_per_kg ?: ""},")
                    .append("${a.srp_online_500g ?: ""},")
                    .append("${a.srp_online_250g ?: ""},")
                    .append("${a.srp_online_100g ?: ""},")
                    .append("${a.srp_reseller_500g ?: ""},")
                    .append("${a.srp_reseller_250g ?: ""},")
                    .append("${a.srp_reseller_100g ?: ""},")
                    .append("${a.srp_offline_500g ?: ""},")
                    .append("${a.srp_offline_250g ?: ""},")
                    .append("${a.srp_offline_100g ?: ""},")
                    .append("${a.srp_online_per_piece ?: ""},")
                    .append("${a.srp_reseller_per_piece ?: ""},")
                    .append("${a.srp_offline_per_piece ?: ""},")
                    .append(csvTextField(deviceId))
                    .append("\n")
            }
        }
        return file
    }
}
