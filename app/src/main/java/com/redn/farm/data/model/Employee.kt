package com.redn.farm.data.model

data class Employee(
    val employee_id: Int = 0,
    val firstname: String,
    val lastname: String,
    val contact: String,
    val date_created: Long = System.currentTimeMillis(),
    val date_updated: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = "$firstname $lastname"
        
    val formattedId: String
        get() = "EMP%04d".format(employee_id)
} 