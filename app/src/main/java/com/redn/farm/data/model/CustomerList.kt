package com.redn.farm.data.model

data class CustomerList(
    val customers: List<CustomerJson>
)

data class CustomerJson(
    val firstname: String,
    val lastname: String,
    val contact: String,
    val customer_type: String,
    val address: String,
    val city: String,
    val province: String,
    val postal_code: String,
    val date_created: String,
    val date_updated: String
) 