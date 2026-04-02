package com.redn.farm.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.redn.farm.data.local.dao.*
import com.redn.farm.data.local.entity.*
import com.redn.farm.data.local.converters.DateTimeConverter
import com.redn.farm.data.local.converters.EnumConverters
import com.redn.farm.data.local.security.PasswordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        ProductPriceEntity::class,
        CustomerEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        EmployeeEntity::class,
        EmployeePaymentEntity::class,
        FarmOperationEntity::class,
        AcquisitionEntity::class,
        RemittanceEntity::class,
        UserEntity::class,
        PricingPresetEntity::class,
        PresetActivationLogEntity::class
    ],
    version = 4
)
@TypeConverters(DateTimeConverter::class, EnumConverters::class)
abstract class FarmDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productPriceDao(): ProductPriceDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun employeePaymentDao(): EmployeePaymentDao
    abstract fun farmOperationDao(): FarmOperationDao
    abstract fun acquisitionDao(): AcquisitionDao
    abstract fun remittanceDao(): RemittanceDao
    abstract fun userDao(): UserDao
    abstract fun pricingPresetDao(): PricingPresetDao
    abstract fun presetActivationLogDao(): PresetActivationLogDao

    companion object {
        const val DATABASE_NAME = "farm_database"
        
        @Volatile
        private var INSTANCE: FarmDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create users table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        role TEXT NOT NULL,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        date_created INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
                        date_updated INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
                    )
                """.trimIndent())

                // Create index for username
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_users_username 
                    ON users (username)
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add discounted price columns to product_prices table
                database.execSQL("""
                    ALTER TABLE product_prices 
                    ADD COLUMN discounted_per_kg_price REAL
                """.trimIndent())
                
                database.execSQL("""
                    ALTER TABLE product_prices 
                    ADD COLUMN discounted_per_piece_price REAL
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): FarmDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d("FarmDatabase", "Starting database initialization...")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FarmDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.d("FarmDatabase", "Database onCreate callback triggered")
                        // Create default users when database is created
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                Log.d("FarmDatabase", "Starting user creation in onCreate callback")
                                val database = getDatabase(context)
                                createDefaultUsers(database)
                            } catch (e: Exception) {
                                Log.e("FarmDatabase", "Error in onCreate callback", e)
                            }
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        Log.d("FarmDatabase", "Database onOpen callback triggered")
                        // Verify users exist when database is opened
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val database = getDatabase(context)
                                val userDao = database.userDao()
                                ensureDemoRoleUsers(userDao)
                                val adminExists = userDao.getUserByUsername("admin")
                                val userExists = userDao.getUserByUsername("user")

                                Log.d(
                                    "FarmDatabase",
                                    "Database opened - Admin exists: ${adminExists != null}, User exists: ${userExists != null}"
                                )

                                if (adminExists == null && userExists == null) {
                                    Log.d("FarmDatabase", "No users found on open, creating default users")
                                    createDefaultUsers(database)
                                }
                            } catch (e: Exception) {
                                Log.e("FarmDatabase", "Error in onOpen callback", e)
                            }
                        }
                    }
                })
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                
                Log.d("FarmDatabase", "Database instance built")
                INSTANCE = instance
                instance
            }
        }

        private suspend fun createDefaultUsers(database: FarmDatabase) {
            val userDao = database.userDao()
            
            try {
                Log.d("FarmDatabase", "Starting createDefaultUsers...")
                
                // First check if users already exist
                val existingAdmin = userDao.getUserByUsername("admin")
                val existingUser = userDao.getUserByUsername("user")
                
                Log.d("FarmDatabase", "Checked existing users - Admin exists: ${existingAdmin != null}, User exists: ${existingUser != null}")
                
                if (existingAdmin != null || existingUser != null) {
                    Log.d("FarmDatabase", "Users already exist, skipping admin/user creation")
                    ensureDemoRoleUsers(userDao)
                    return
                }

                // Create admin user
                val adminPassword = "admin123"
                val adminHash = PasswordManager.hashPassword(adminPassword)
                Log.d("FarmDatabase", "Created admin password hash: ${adminHash.take(10)}...")
                
                val adminUser = UserEntity(
                    username = "admin",
                    password_hash = adminHash,
                    full_name = "Administrator",
                    role = "ADMIN",
                    is_active = true,
                    date_created = System.currentTimeMillis(),
                    date_updated = System.currentTimeMillis()
                )

                // Create regular user
                val userPassword = "user123"
                val userHash = PasswordManager.hashPassword(userPassword)
                Log.d("FarmDatabase", "Created user password hash: ${userHash.take(10)}...")
                
                val regularUser = UserEntity(
                    username = "user",
                    password_hash = userHash,
                    full_name = "Regular User",
                    role = "USER",
                    is_active = true,
                    date_created = System.currentTimeMillis(),
                    date_updated = System.currentTimeMillis()
                )

                // Insert users and get their IDs
                val adminId = userDao.insertUser(adminUser)
                val userId = userDao.insertUser(regularUser)

                Log.d("FarmDatabase", "Inserted users - Admin ID: $adminId, User ID: $userId")

                // Verify users were created
                val verifyAdmin = userDao.getUserByUsername("admin")
                val verifyUser = userDao.getUserByUsername("user")

                Log.d("FarmDatabase", "Verification - Admin exists: ${verifyAdmin != null}, User exists: ${verifyUser != null}")
                Log.d("FarmDatabase", "Admin hash matches: ${verifyAdmin?.password_hash == adminHash}")
                Log.d("FarmDatabase", "User hash matches: ${verifyUser?.password_hash == userHash}")

                if (verifyAdmin != null && verifyUser != null) {
                    Log.d("FarmDatabase", "Successfully verified both users exist in database")
                } else {
                    Log.e("FarmDatabase", "User verification failed. Admin exists: ${verifyAdmin != null}, User exists: ${verifyUser != null}")
                }

                ensureDemoRoleUsers(userDao)

            } catch (e: Exception) {
                Log.e("FarmDatabase", "Error creating default users", e)
                e.printStackTrace()
            }
        }

        /**
         * Demo accounts for each RBAC role (passwords are for local/dev only).
         * Idempotent: inserts only when username is missing.
         */
        private suspend fun ensureDemoRoleUsers(userDao: UserDao) {
            val seeds = listOf(
                DemoRoleUser("store", "store123", "Store Assistant (demo)", "STORE_ASSISTANT"),
                DemoRoleUser("purchasing", "purchase123", "Purchasing Assistant (demo)", "PURCHASING"),
                DemoRoleUser("farmer", "farmer123", "Farmer (demo)", "FARMER")
            )
            for (seed in seeds) {
                if (userDao.getUserByUsername(seed.username) != null) continue
                userDao.insertUser(
                    UserEntity(
                        username = seed.username,
                        password_hash = PasswordManager.hashPassword(seed.plainPassword),
                        full_name = seed.fullName,
                        role = seed.role,
                        is_active = true,
                        date_created = System.currentTimeMillis(),
                        date_updated = System.currentTimeMillis()
                    )
                )
                Log.d("FarmDatabase", "Seeded demo user ${seed.username} (${seed.role})")
            }
        }

        private data class DemoRoleUser(
            val username: String,
            val plainPassword: String,
            val fullName: String,
            val role: String
        )

        fun getDatabaseFile(context: Context) = context.getDatabasePath(DATABASE_NAME)

        fun doesDatabaseExist(context: Context): Boolean {
            return getDatabaseFile(context).exists()
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
} 