package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.OrderEntity
import com.redn.farm.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

@Dao
interface OrderDao {
    @Transaction
    @Query("""
        SELECT o.*, 
            c.firstname as customer_firstname,
            c.lastname as customer_lastname,
            c.contact as customer_contact
        FROM orders o
        INNER JOIN customers c ON o.customer_id = c.customer_id
        ORDER BY o.order_date DESC
    """)
    fun getAllOrders(): Flow<List<OrderWithDetails>>

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun createOrder(order: OrderEntity, items: List<OrderItemEntity>): Long {
        val orderId = insertOrder(order)
        insertOrderItems(items.map { it.copy(order_id = orderId.toInt()) })
        return orderId
    }

    @Query("""
        SELECT oi.*, p.product_name 
        FROM order_items oi
        INNER JOIN products p ON oi.product_id = p.product_id
        WHERE oi.order_id = :orderId
    """)
    fun getOrderItems(orderId: Int): Flow<List<OrderItemWithProduct>>

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("UPDATE orders SET is_paid = :isPaid WHERE order_id = :orderId")
    suspend fun updateOrderPaymentStatus(orderId: Int, isPaid: Boolean)

    @Query("DELETE FROM orders WHERE order_id = :orderId AND is_paid = 0")
    suspend fun deleteUnpaidOrder(orderId: Int)

    @Query("""
        SELECT o.*, 
            c.firstname as customer_firstname,
            c.lastname as customer_lastname,
            c.contact as customer_contact
        FROM orders o
        INNER JOIN customers c ON o.customer_id = c.customer_id
        WHERE o.order_id = :orderId
    """)
    fun getOrderById(orderId: Int): Flow<OrderWithDetails?>

    @Transaction
    suspend fun updateOrderWithItems(
        order: OrderEntity,
        items: List<OrderItemEntity>
    ) {
        updateOrder(order)
        // Delete existing items
        deleteOrderItems(order.order_id)
        // Insert new items
        insertOrderItems(items)
    }

    @Query("DELETE FROM order_items WHERE order_id = :orderId")
    suspend fun deleteOrderItems(orderId: Int)

    @Query("""
        SELECT oi.*, p.product_name 
        FROM order_items oi
        INNER JOIN products p ON oi.product_id = p.product_id
    """)
    fun getAllOrderItems(): Flow<List<OrderItemWithProduct>>

    @Query("DELETE FROM orders")
    suspend fun truncateOrders()

    @Query("DELETE FROM order_items")
    suspend fun truncateOrderItems()

    @Transaction
    suspend fun truncate() {
        truncateOrderItems()
        truncateOrders()
    }

    @Transaction
    suspend fun deleteOrderAndItems(orderId: Int) {
        // Only delete if the order is unpaid
        val order = getOrderById(orderId).first()
        if (order?.order?.is_paid == false) {
            deleteOrderItems(orderId)
            deleteUnpaidOrder(orderId)
        }
    }
}

data class OrderWithDetails(
    @Embedded val order: OrderEntity,
    @ColumnInfo(name = "customer_firstname") val customerFirstname: String,
    @ColumnInfo(name = "customer_lastname") val customerLastname: String,
    @ColumnInfo(name = "customer_contact") val customerContact: String
) {
    val customerName: String
        get() = "$customerFirstname $customerLastname"
}

data class OrderItemWithProduct(
    @Embedded val orderItem: OrderItemEntity,
    val product_name: String
) 