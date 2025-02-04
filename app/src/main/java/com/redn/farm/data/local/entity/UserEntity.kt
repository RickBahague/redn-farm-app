package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val user_id: Int = 0,
    val username: String,
    val password_hash: String,  // Store hashed password, never plain text
    val full_name: String,
    val role: String,
    val is_active: Boolean = true,
    val date_created: Long = System.currentTimeMillis(),
    val date_updated: Long = System.currentTimeMillis()
) 