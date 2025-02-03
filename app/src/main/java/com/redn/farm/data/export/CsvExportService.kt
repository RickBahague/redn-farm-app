package com.redn.farm.data.export

import android.content.Context
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
    
    fun exportCustomers(customers: List<Customer>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "customers_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("CustomerId,FirstName,LastName,Contact,CustomerType,Address,City,Province,PostalCode,DateCreated,DateUpdated,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            customers.forEach { customer ->
                writer.append("${customer.customer_id},")
                    .append("\"${customer.firstname}\",")
                    .append("\"${customer.lastname}\",")
                    .append("\"${customer.contact}\",")
                    .append("\"${customer.customer_type}\",")
                    .append("\"${customer.address}\",")
                    .append("\"${customer.city}\",")
                    .append("\"${customer.province}\",")
                    .append("\"${customer.postal_code}\",")
                    .append("${customer.date_created},")
                    .append("${customer.date_updated},")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }
    
    fun exportOrders(orders: List<Order>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "orders_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("OrderId,CustomerId,CustomerName,CustomerContact,TotalAmount,OrderDate,UpdateDate,IsPaid,IsDelivered,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            orders.forEach { order ->
                writer.append("${order.order_id},")
                    .append("${order.customer_id},")
                    .append("\"${order.customerName}\",")
                    .append("\"${order.customerContact}\",")
                    .append("${order.total_amount},")
                    .append("${order.order_date},")
                    .append("${order.order_update_date},")
                    .append("${order.is_paid},")
                    .append("${order.is_delivered},")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }
    
    fun exportOrderItems(orderItems: List<OrderItem>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "order_items_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("Id,OrderId,ProductId,ProductName,Quantity,PricePerUnit,IsPerKg,TotalPrice,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            orderItems.forEach { item ->
                writer.append("${item.id},")
                    .append("${item.order_id},")
                    .append("${item.product_id},")
                    .append("\"${item.product_name}\",")
                    .append("${item.quantity},")
                    .append("${item.price_per_unit},")
                    .append("${item.is_per_kg},")
                    .append("${item.total_price},")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }
    
    fun exportEmployeePayments(payments: List<EmployeePayment>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "employee_payments_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("PaymentId,EmployeeId,EmployeeName,Amount,CashAdvanceAmount,LiquidatedAmount,DatePaid,ReceivedDate,Signature,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            payments.forEach { payment ->
                writer.append("${payment.payment_id},")
                    .append("${payment.employee_id},")
                    .append("\"${payment.employeeName}\",")
                    .append("${payment.amount},")
                    .append("${payment.cash_advance_amount ?: ""},")
                    .append("${payment.liquidated_amount ?: ""},")
                    .append("${payment.date_paid},")
                    .append("${payment.received_date ?: ""},")
                    .append("\"${payment.signature}\",")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }

    fun exportEmployees(employees: List<Employee>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "employees_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("EmployeeId,FirstName,LastName,Contact,DateCreated,DateUpdated,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            employees.forEach { employee ->
                writer.append("${employee.employee_id},")
                    .append("\"${employee.firstname}\",")
                    .append("\"${employee.lastname}\",")
                    .append("\"${employee.contact}\",")
                    .append("${employee.date_created},")
                    .append("${employee.date_updated},")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }

    fun exportFarmOperations(operations: List<FarmOperation>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "farm_operations_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("OperationId,OperationType,OperationDate,Details,Area,WeatherCondition,Personnel,ProductId,ProductName,DateCreated,DateUpdated,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            operations.forEach { operation ->
                writer.append("${operation.operation_id},")
                    .append("\"${operation.operation_type}\",")
                    .append("${operation.operation_date},")
                    .append("\"${operation.details}\",")
                    .append("\"${operation.area}\",")
                    .append("\"${operation.weather_condition}\",")
                    .append("\"${operation.personnel}\",")
                    .append("${operation.product_id ?: ""},")
                    .append("\"${operation.product_name}\",")
                    .append("${operation.date_created},")
                    .append("${operation.date_updated},")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }

    fun exportProductPrices(prices: List<ProductPrice>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "product_prices_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("PriceId,ProductId,PerKgPrice,PerPiecePrice,DateCreated,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            prices.forEach { price ->
                writer.append("${price.price_id},")
                    .append("${price.product_id},")
                    .append("${price.per_kg_price ?: ""},")
                    .append("${price.per_piece_price ?: ""},")
                    .append("${price.date_created},")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }

    fun exportProducts(products: List<Product>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "products_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("ProductId,ProductName,ProductDescription,UnitType,IsActive,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            products.forEach { product ->
                writer.append("${product.product_id},")
                    .append("\"${product.product_name}\",")
                    .append("\"${product.product_description}\",")
                    .append("\"${product.unit_type}\",")
                    .append("${product.is_active},")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }

    fun exportRemittances(remittances: List<Remittance>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "remittances_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("RemittanceId,Amount,Date,Remarks,DateUpdated,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            remittances.forEach { remittance ->
                writer.append("${remittance.remittance_id},")
                    .append("${remittance.amount},")
                    .append("${remittance.date},")
                    .append("\"${remittance.remarks}\",")
                    .append("${remittance.date_updated},")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }

    fun exportAcquisitions(acquisitions: List<Acquisition>): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "acquisitions_$timestamp.csv"
        val file = File(getExportsDirectory(), fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append("AcquisitionId,ProductId,ProductName,Quantity,PricePerUnit,TotalAmount,IsPerKg,DateAcquired,Location,DeviceId\n")
            
            // Write data
            val deviceId = DeviceUtils.getDeviceId(context)
            acquisitions.forEach { acquisition ->
                writer.append("${acquisition.acquisition_id},")
                    .append("${acquisition.product_id},")
                    .append("\"${acquisition.product_name}\",")
                    .append("${acquisition.quantity},")
                    .append("${acquisition.price_per_unit},")
                    .append("${acquisition.total_amount},")
                    .append("${acquisition.is_per_kg},")
                    .append("${acquisition.date_acquired},")
                    .append("\"${acquisition.location}\",")
                    .append("$deviceId\n")
            }
        }
        
        return file
    }
} 