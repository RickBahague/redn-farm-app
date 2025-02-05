package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.OrderDao
import com.redn.farm.data.local.entity.OrderEntity
import com.redn.farm.data.local.entity.OrderItemEntity
import com.redn.farm.data.model.Order
import com.redn.farm.data.model.OrderItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneId

class OrderRepository(private val orderDao: OrderDao) {
    fun getAllOrders(): Flow<List<Order>> {
        return orderDao.getAllOrders().map { orders ->
            orders.map { orderWithDetails ->
                Order(
                    order_id = orderWithDetails.order.order_id,
                    customer_id = orderWithDetails.order.customer_id,
                    total_amount = orderWithDetails.order.total_amount,
                    order_date = orderWithDetails.order.order_date,
                    order_update_date = orderWithDetails.order.order_update_date,
                    customerName = "${orderWithDetails.customerFirstname} ${orderWithDetails.customerLastname}",
                    customerContact = orderWithDetails.customerContact,
                    is_paid = orderWithDetails.order.is_paid,
                    is_delivered = orderWithDetails.order.is_delivered
                )
            }
        }
    }

    suspend fun createOrder(order: Order, items: List<OrderItem>): Long {
        val orderEntity = OrderEntity(
            customer_id = order.customer_id,
            total_amount = order.total_amount,
            is_delivered = order.is_delivered
        )

        val orderItems = items.map { item ->
            OrderItemEntity(
                product_id = item.product_id,
                quantity = item.quantity,
                price_per_unit = item.price_per_unit,
                is_per_kg = item.is_per_kg,
                total_price = item.total_price,
                order_id = 0 // Will be set by DAO
            )
        }

        return orderDao.createOrder(orderEntity, orderItems)
    }

    suspend fun updateOrderPaymentStatus(orderId: Int, isPaid: Boolean) {
        orderDao.updateOrderPaymentStatus(orderId, isPaid)
    }

    suspend fun deleteOrder(orderId: Int) {
        // Use the transaction method that deletes both order and its items
        orderDao.deleteOrderAndItems(orderId)
    }

    suspend fun truncate() {
        orderDao.truncate()
    }

    fun getOrderById(orderId: Int): Flow<Order?> {
        return orderDao.getOrderById(orderId).map { orderWithDetails ->
            orderWithDetails?.let {
                Order(
                    order_id = it.order.order_id,
                    customer_id = it.order.customer_id,
                    total_amount = it.order.total_amount,
                    order_date = it.order.order_date,
                    order_update_date = it.order.order_update_date,
                    customerName = "${it.customerFirstname} ${it.customerLastname}",
                    customerContact = it.customerContact,
                    is_paid = it.order.is_paid,
                    is_delivered = it.order.is_delivered
                )
            }
        }
    }

    fun getOrderItems(orderId: Int): Flow<List<OrderItem>> {
        return orderDao.getOrderItems(orderId).map { items ->
            items.map { item ->
                OrderItem(
                    id = item.orderItem.id,
                    order_id = item.orderItem.order_id,
                    product_id = item.orderItem.product_id,
                    product_name = item.product_name,
                    quantity = item.orderItem.quantity,
                    price_per_unit = item.orderItem.price_per_unit,
                    is_per_kg = item.orderItem.is_per_kg,
                    total_price = item.orderItem.total_price
                )
            }
        }
    }

    suspend fun updateOrder(order: Order, items: List<OrderItem>) {
        val orderEntity = OrderEntity(
            order_id = order.order_id,
            customer_id = order.customer_id,
            total_amount = order.total_amount,
            order_date = order.order_date,
            order_update_date = System.currentTimeMillis(),
            is_paid = order.is_paid,
            is_delivered = order.is_delivered
        )

        val orderItemEntities = items.map { item ->
            OrderItemEntity(
                id = item.id,
                order_id = item.order_id,
                product_id = item.product_id,
                quantity = item.quantity,
                price_per_unit = item.price_per_unit,
                is_per_kg = item.is_per_kg,
                total_price = item.total_price
            )
        }

        orderDao.updateOrder(orderEntity)
        orderDao.deleteOrderItems(order.order_id)
        orderDao.insertOrderItems(orderItemEntities)
    }

    fun getAllOrderItems(): Flow<List<OrderItem>> {
        return orderDao.getAllOrderItems().map { items ->
            items.map { item ->
                OrderItem(
                    id = item.orderItem.id,
                    order_id = item.orderItem.order_id,
                    product_id = item.orderItem.product_id,
                    product_name = item.product_name,
                    quantity = item.orderItem.quantity,
                    price_per_unit = item.orderItem.price_per_unit,
                    is_per_kg = item.orderItem.is_per_kg,
                    total_price = item.orderItem.total_price
                )
            }
        }
    }
} 