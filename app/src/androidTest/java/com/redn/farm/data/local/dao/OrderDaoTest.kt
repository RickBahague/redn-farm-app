package com.redn.farm.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.entity.CustomerEntity
import com.redn.farm.data.local.entity.OrderEntity
import com.redn.farm.data.local.entity.OrderItemEntity
import com.redn.farm.data.local.entity.ProductEntity
import com.redn.farm.data.model.CustomerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for:
 *   BUG-02 — ExportViewModel.truncateOrderItems() calls orderRepository.truncate()
 *             which deletes orders AND items, not just items.
 *
 * Run with: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.redn.farm.data.local.dao.OrderDaoTest
 */
@RunWith(AndroidJUnit4::class)
class OrderDaoTest {

    private lateinit var db: FarmDatabase
    private lateinit var orderDao: OrderDao
    private lateinit var customerDao: CustomerDao
    private lateinit var productDao: ProductDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FarmDatabase::class.java
        ).allowMainThreadQueries().build()

        orderDao = db.orderDao()
        customerDao = db.customerDao()
        productDao = db.productDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private suspend fun insertSeedData(): Pair<OrderEntity, List<OrderItemEntity>> {
        customerDao.insertCustomer(
            CustomerEntity(
                customer_id = 1,
                firstname = "Test",
                lastname = "Customer",
                contact = "09000000000",
                customer_type = CustomerType.RETAIL,
                address = "", city = "", province = "", postal_code = ""
            )
        )
        productDao.insertProduct(
            ProductEntity(
                product_id = "PROD001",
                product_name = "Tomato",
                product_description = "",
                unit_type = "kg"
            )
        )

        val order = OrderEntity(customer_id = 1, total_amount = 500.0)
        val orderId = orderDao.insertOrder(order).toInt()

        val items = listOf(
            OrderItemEntity(
                order_id = orderId,
                product_id = "PROD001",
                quantity = 5.0,
                price_per_unit = 100.0,
                is_per_kg = true,
                total_price = 500.0
            )
        )
        orderDao.insertOrderItems(items)

        return Pair(order.copy(order_id = orderId), items)
    }

    // -------------------------------------------------------------------------
    // BUG-02 — documents the broken behaviour
    // -------------------------------------------------------------------------

    @Test
    fun truncate_removesOrdersAndItems_documentingBug02() = runTest {
        insertSeedData()

        // This is what ExportViewModel.truncateOrderItems() incorrectly calls
        orderDao.truncate()

        val ordersAfter = orderDao.getAllOrders().first()
        assertTrue(
            "BUG-02 confirmed: orderDao.truncate() deletes orders (${ordersAfter.size} remaining). " +
            "Fix: add a separate truncateOrderItemsOnly() method.",
            ordersAfter.isEmpty()
        )
    }

    // -------------------------------------------------------------------------
    // BUG-02 — regression test that must PASS after the fix is applied.
    // Currently this test fails to compile because truncateOrderItemsOnly()
    // does not exist yet. Uncomment after adding the method.
    // -------------------------------------------------------------------------

    /*
    @Test
    fun truncateOrderItemsOnly_preservesOrders() = runTest {
        val (_, _) = insertSeedData()

        // After the fix: this should only clear order_items, not orders
        orderDao.truncateOrderItemsOnly()

        val ordersAfter = orderDao.getAllOrders().first()
        assertEquals("Orders must survive truncateOrderItemsOnly()", 1, ordersAfter.size)

        val itemsAfter = orderDao.getAllOrderItems().first()
        assertTrue("Order items should be empty after truncateOrderItemsOnly()", itemsAfter.isEmpty())
    }
    */

    // -------------------------------------------------------------------------
    // Existing correct behaviour — protect against regressions
    // -------------------------------------------------------------------------

    @Test
    fun createOrder_persistsOrderAndItems() = runTest {
        val (order, items) = insertSeedData()

        val allOrders = orderDao.getAllOrders().first()
        assertEquals(1, allOrders.size)

        val allItems = orderDao.getAllOrderItems().first()
        assertEquals(1, allItems.size)
        assertEquals(items[0].product_id, allItems[0].orderItem.product_id)
    }

    @Test
    fun deleteOrderAndItems_onlyDeletesUnpaidOrders() = runTest {
        val (order, _) = insertSeedData()
        val orderId = order.order_id

        // Mark paid
        orderDao.updateOrderPaymentStatus(orderId, true)

        // Attempt to delete — should do nothing because order is paid
        orderDao.deleteOrderAndItems(orderId)

        val ordersAfter = orderDao.getAllOrders().first()
        assertEquals("Paid orders must not be deleted", 1, ordersAfter.size)
    }

    @Test
    fun deleteOrderAndItems_deletesUnpaidOrder() = runTest {
        val (order, _) = insertSeedData()
        val orderId = order.order_id

        // Order is unpaid by default
        orderDao.deleteOrderAndItems(orderId)

        val ordersAfter = orderDao.getAllOrders().first()
        assertTrue("Unpaid order should be deleted", ordersAfter.isEmpty())

        val itemsAfter = orderDao.getAllOrderItems().first()
        assertTrue("Order items should cascade-delete with the order", itemsAfter.isEmpty())
    }

    @Test
    fun updateOrderWithItems_replacesItems() = runTest {
        val (order, _) = insertSeedData()
        val orderId = order.order_id

        val updatedOrder = order.copy(total_amount = 300.0)
        val newItems = listOf(
            OrderItemEntity(
                order_id = orderId,
                product_id = "PROD001",
                quantity = 3.0,
                price_per_unit = 100.0,
                is_per_kg = true,
                total_price = 300.0
            )
        )

        orderDao.updateOrderWithItems(updatedOrder, newItems)

        val items = orderDao.getAllOrderItems().first()
        assertEquals(1, items.size)
        assertEquals(3.0, items[0].orderItem.quantity, 0.001)
    }
}
