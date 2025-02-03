package com.redn.farm.data.model

data class Remittance(
    val remittance_id: Int = 0,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val remarks: String = "",
    val date_updated: Long = System.currentTimeMillis()
)