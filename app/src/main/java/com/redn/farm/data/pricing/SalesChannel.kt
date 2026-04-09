package com.redn.farm.data.pricing

import com.redn.farm.data.model.CustomerType

/** Stored on [com.redn.farm.data.model.Order.channel] (ORD-US-01). */
object SalesChannel {
    const val ONLINE = "online"
    const val RESELLER = "reseller"
    const val OFFLINE = "offline"

    val ALL = listOf(ONLINE, RESELLER, OFFLINE)

    fun label(key: String): String = when (key) {
        ONLINE -> "Online"
        RESELLER -> "Reseller"
        OFFLINE -> "Store (offline)"
        else -> key
    }

    fun normalize(raw: String): String =
        when (raw.lowercase()) {
            ONLINE -> ONLINE
            RESELLER -> RESELLER
            else -> OFFLINE
        }
}

fun CustomerType.defaultOrderChannel(): String = when (this) {
    CustomerType.WHOLESALE -> SalesChannel.RESELLER
    CustomerType.RETAIL, CustomerType.REGULAR -> SalesChannel.OFFLINE
}
