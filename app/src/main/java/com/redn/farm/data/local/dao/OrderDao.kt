package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.OrderEntity
import com.redn.farm.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    // ─── EOD aggregation queries (Phase 2b) ──────────────────────────────────

    /**
     * Total and collected revenue for orders whose order_date falls within [startMillis, endMillis].
     * Used by DayCloseRepository to compute today's revenue snapshot.
     */
    @Query("""
        SELECT
            COUNT(*)               AS order_count,
            SUM(total_amount)      AS total_sales,
            SUM(CASE WHEN is_paid = 1 THEN total_amount ELSE 0 END) AS total_collected
        FROM orders
        WHERE order_date BETWEEN :startMillis AND :endMillis
    """)
    suspend fun getSalesSummaryOnDate(startMillis: Long, endMillis: Long): OrderSalesSummary

    /**
     * Count and sum of orders that are still unpaid as of [endMillis].
     */
    @Query("""
        SELECT COUNT(*) AS count, COALESCE(SUM(total_amount), 0) AS amount
        FROM orders
        WHERE is_paid = 0 AND order_date <= :endMillis
    """)
    suspend fun getUnpaidSummaryAsOf(endMillis: Long): UnpaidSummary

    /**
     * Per-product sold quantity (sum of order_items.quantity) for orders
     * within [startMillis, endMillis]. Used for COGS and inventory reconciliation.
     */
    @Query("""
        SELECT oi.product_id, SUM(oi.quantity) AS total_qty
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.order_id
        WHERE o.order_date BETWEEN :startMillis AND :endMillis
        GROUP BY oi.product_id
    """)
    suspend fun getSoldQtyByProductOnDate(
        startMillis: Long,
        endMillis: Long
    ): List<ProductQtySummary>

    /**
     * Per-product sold quantity (all time up to and including [endMillis]).
     * Used for the all-time running stock ledger (EOD-US-03).
     */
    @Query("""
        SELECT oi.product_id, SUM(oi.quantity) AS total_qty
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.order_id
        WHERE o.order_date <= :endMillis
        GROUP BY oi.product_id
    """)
    suspend fun getTotalSoldQtyByProductUpTo(endMillis: Long): List<ProductQtySummary>

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

// EOD result types

data class OrderSalesSummary(
    val order_count: Int,
    val total_sales: Double,
    val total_collected: Double,
)

data class UnpaidSummary(
    val count: Int,
    val amount: Double,
)

data class ProductQtySummary(
    val product_id: String,
    val total_qty: Double,
)