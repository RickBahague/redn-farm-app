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

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN is_paid = 1 THEN 1 ELSE 0 END), 0) AS paid_count,
            COALESCE(SUM(CASE WHEN is_paid = 1 THEN total_amount ELSE 0 END), 0) AS paid_amount,
            COALESCE(SUM(CASE WHEN is_paid = 0 THEN 1 ELSE 0 END), 0) AS unpaid_count,
            COALESCE(SUM(CASE WHEN is_paid = 0 THEN total_amount ELSE 0 END), 0) AS unpaid_amount,
            COALESCE(SUM(CASE WHEN is_delivered = 1 THEN 1 ELSE 0 END), 0) AS delivered_count
        FROM orders
        WHERE order_date BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun getDailyOrderBreakdownOnDate(
        startMillis: Long,
        endMillis: Long,
    ): DailyOrderBreakdown

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
     * Per-product sold quantities split by line unit (kg vs piece) for orders
     * within [startMillis, endMillis]. Used for COGS (WAC is per kg) and inventory ledger.
     */
    @Query(
        """
        SELECT oi.product_id,
            COALESCE(SUM(CASE WHEN oi.is_per_kg = 1 THEN oi.quantity ELSE 0 END), 0) AS qty_kg_sold,
            COALESCE(SUM(CASE WHEN oi.is_per_kg = 0 THEN oi.quantity ELSE 0 END), 0) AS qty_pc_sold
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.order_id
        WHERE o.order_date BETWEEN :startMillis AND :endMillis
        GROUP BY oi.product_id
        """
    )
    suspend fun getSoldQtyBreakdownByProductOnDate(
        startMillis: Long,
        endMillis: Long,
    ): List<ProductSoldQtyBreakdown>

    /** Per-product line revenue on the business date (aligns with **Top products** / inventory **Sold today**). */
    @Query(
        """
        SELECT oi.product_id, COALESCE(SUM(oi.total_price), 0) AS revenue
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.order_id
        WHERE o.order_date BETWEEN :startMillis AND :endMillis
        GROUP BY oi.product_id
        """
    )
    suspend fun getProductRevenueOnDate(
        startMillis: Long,
        endMillis: Long,
    ): List<ProductDayRevenueRow>

    /**
     * Same as [getSoldQtyBreakdownByProductOnDate] but all orders up to and including [endMillis].
     */
    @Query(
        """
        SELECT oi.product_id,
            COALESCE(SUM(CASE WHEN oi.is_per_kg = 1 THEN oi.quantity ELSE 0 END), 0) AS qty_kg_sold,
            COALESCE(SUM(CASE WHEN oi.is_per_kg = 0 THEN oi.quantity ELSE 0 END), 0) AS qty_pc_sold
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.order_id
        WHERE o.order_date <= :endMillis
        GROUP BY oi.product_id
        """
    )
    suspend fun getTotalSoldQtyBreakdownByProductUpTo(endMillis: Long): List<ProductSoldQtyBreakdown>

    @Query(
        """
        SELECT channel,
            COUNT(*) AS order_count,
            COALESCE(SUM(total_amount), 0) AS total_sales,
            COALESCE(SUM(CASE WHEN is_paid = 1 THEN total_amount ELSE 0 END), 0) AS total_collected
        FROM orders
        WHERE order_date BETWEEN :startMillis AND :endMillis
        GROUP BY channel
        """
    )
    suspend fun getSalesByChannel(startMillis: Long, endMillis: Long): List<ChannelSalesRow>

    @Query(
        """
        SELECT oi.product_id, p.product_name,
            COALESCE(SUM(CASE WHEN oi.is_per_kg = 1 THEN oi.quantity ELSE 0 END), 0) AS qty_kg_sold,
            COALESCE(SUM(CASE WHEN oi.is_per_kg = 0 THEN oi.quantity ELSE 0 END), 0) AS qty_pc_sold,
            COALESCE(SUM(oi.total_price), 0) AS revenue
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.order_id
        INNER JOIN products p ON oi.product_id = p.product_id
        WHERE o.order_date BETWEEN :startMillis AND :endMillis
        GROUP BY oi.product_id
        ORDER BY revenue DESC
        LIMIT :limit
        """
    )
    suspend fun getTopProductsByRevenue(
        startMillis: Long,
        endMillis: Long,
        limit: Int,
    ): List<ProductTopRevenueRow>

    @Query(
        """
        SELECT COUNT(*) FROM orders
        WHERE is_paid = 0 AND order_date BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun getUnpaidOrderCountOnDate(startMillis: Long, endMillis: Long): Int

    @Query(
        """
        SELECT COALESCE(SUM(total_amount), 0) FROM orders
        WHERE is_paid = 1 AND channel IN ('offline', 'reseller')
        AND order_date BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun getPaidOfflineResellerTotalOnDate(startMillis: Long, endMillis: Long): Double

    @Query(
        """
        SELECT COUNT(*) AS order_count, COALESCE(SUM(total_amount), 0) AS total_amount
        FROM orders
        WHERE is_paid = 1 AND channel = 'online'
        AND order_date BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun getDigitalCollectionsOnDate(startMillis: Long, endMillis: Long): DigitalCollectionsSummary

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE is_paid = 1")
    suspend fun getTotalCollectedAllTime(): Double

    @Transaction
    @Query(
        """
        SELECT o.*,
            c.firstname AS customer_firstname,
            c.lastname AS customer_lastname,
            c.contact AS customer_contact
        FROM orders o
        INNER JOIN customers c ON o.customer_id = c.customer_id
        WHERE o.is_paid = 0
        ORDER BY o.order_date ASC
        """
    )
    suspend fun getAllUnpaidOrdersOldestFirst(): List<OrderWithDetails>

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

data class DailyOrderBreakdown(
    val paid_count: Int,
    val paid_amount: Double,
    val unpaid_count: Int,
    val unpaid_amount: Double,
    val delivered_count: Int,
)

data class UnpaidSummary(
    val count: Int,
    val amount: Double,
)

/** Per-product sold qty split by order line unit — convert pc → kg via acquisition piece_count in repository. */
data class ProductSoldQtyBreakdown(
    val product_id: String,
    val qty_kg_sold: Double,
    val qty_pc_sold: Double,
)

data class ProductDayRevenueRow(
    val product_id: String,
    val revenue: Double,
)

data class ChannelSalesRow(
    val channel: String,
    val order_count: Int,
    val total_sales: Double,
    val total_collected: Double,
)

data class ProductTopRevenueRow(
    val product_id: String,
    val product_name: String,
    val qty_kg_sold: Double,
    val qty_pc_sold: Double,
    val revenue: Double,
)

data class DigitalCollectionsSummary(
    val order_count: Int,
    val total_amount: Double,
)