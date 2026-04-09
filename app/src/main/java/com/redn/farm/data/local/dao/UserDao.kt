package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username AND is_active = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    /** Username lookup ignoring active flag (signup / admin checks). */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET is_active = 0 WHERE user_id = :userId")
    suspend fun deactivateUser(userId: Int)

    @Query("DELETE FROM users")
    suspend fun truncate()

    /** EXP-US-02: remove custom accounts; keep default **admin** / **user** seed logins. */
    @Query(
        """DELETE FROM users WHERE LOWER(username) NOT IN ('admin', 'user')"""
    )
    suspend fun deleteNonSeedUsers()
} 