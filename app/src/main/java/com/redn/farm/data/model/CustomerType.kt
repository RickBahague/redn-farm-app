package com.redn.farm.data.model

enum class CustomerType {
    REGULAR,
    RETAIL,
    WHOLESALE;

    override fun toString(): String {
        return when (this) {
            REGULAR -> "Regular"
            RETAIL -> "Retail"
            WHOLESALE -> "Wholesale"
        }
    }
} 