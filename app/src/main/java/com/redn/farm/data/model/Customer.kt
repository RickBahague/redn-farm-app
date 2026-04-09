package com.redn.farm.data.model

data class Customer(
    val customer_id: Int = 0,
    val firstname: String,
    val lastname: String,
    val contact: String,
    val customer_type: CustomerType = CustomerType.RETAIL,
    val address: String = "",
    val city: String = "",
    val province: String = "",
    val postal_code: String = "",
    val date_created: Long = System.currentTimeMillis(),
    val date_updated: Long = System.currentTimeMillis(),
    val fullName: String = "$firstname $lastname"
) {
    val fullAddress: String
        get() = buildString {
            append(address)
            if (city.isNotBlank()) append(", $city")
            if (province.isNotBlank()) append(", $province")
            if (postal_code.isNotBlank()) append(" $postal_code")
        }.trimStart(',', ' ')
}
