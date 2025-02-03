package com.redn.farm.data.local

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.redn.farm.data.model.ProductList
import com.redn.farm.data.model.ProductPriceList
import com.redn.farm.data.model.CustomerList
import com.redn.farm.data.model.CustomerType
import com.redn.farm.data.local.entity.CustomerEntity
import com.redn.farm.data.local.entity.ProductEntity
import com.redn.farm.data.local.entity.ProductPriceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class DatabaseInitializer(private val context: Context) {
    private val gson: Gson = GsonBuilder()
        .create()  // Remove date format since we'll handle it manually
    private val applicationScope = CoroutineScope(SupervisorJob())

    val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("DatabaseInitializer", "Database creation started")
            
            // Create tables
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS products (
                    product_id TEXT PRIMARY KEY NOT NULL,
                    product_name TEXT NOT NULL,
                    product_description TEXT NOT NULL,
                    unit_type TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())
            
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS product_prices (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id TEXT NOT NULL,
                    per_kg_price REAL,
                    per_piece_price REAL,
                    valid_date TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(product_id) REFERENCES products(product_id) ON DELETE CASCADE
                )
            """.trimIndent())
            
            applicationScope.launch(Dispatchers.IO) {
                try {
                    populateDatabase()
                    Log.d("DatabaseInitializer", "Database initialized successfully")
                } catch (e: Exception) {
                    Log.e("DatabaseInitializer", "Error initializing database", e)
                }
            }
        }
    }

    // New function to manually trigger reinitialization
    suspend fun reinitializeDatabase() = withContext(Dispatchers.IO) {
        try {
            Log.d("DatabaseInitializer", "Manual database reinitialization started")
            
            // Get the current database instance and close it
            FarmDatabase.getDatabase(context).close()
            FarmDatabase.clearInstance()
            
            // Delete database file
            context.deleteDatabase("farm_database")
            Log.d("DatabaseInitializer", "Existing database deleted")
            
            // Create new database instance
            val database = FarmDatabase.getDatabase(context)
            
            // Initialize with data
            populateDatabase()
            
            Log.d("DatabaseInitializer", "Database reinitialized successfully")
            true // Return success
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error reinitializing database", e)
            throw e
        }
    }

    private suspend fun populateDatabase() {
        val database = FarmDatabase.getDatabase(context)

        try {
            // Populate products
            val products = listOf(
                ProductEntity(
                    product_id = "PROD001",
                    product_name = "Tomatoes",
                    product_description = "Fresh red tomatoes",
                    unit_type = "kg",
                    is_active = true
                ),
                ProductEntity(
                    product_id = "PROD002",
                    product_name = "Lettuce",
                    product_description = "Fresh green lettuce",
                    unit_type = "piece",
                    is_active = true
                )
                // Add more products as needed
            )
            
            products.forEach { product ->
                database.productDao().insertProduct(product)
            }

            // Populate product prices
            val productPrices = listOf(
                ProductPriceEntity(
                    product_id = "PROD001",
                    per_kg_price = 50.0,
                    per_piece_price = null,
                    date_created = LocalDateTime.now()
                ),
                ProductPriceEntity(
                    product_id = "PROD002",
                    per_kg_price = null,
                    per_piece_price = 25.0,
                    date_created = LocalDateTime.now()
                )
                // Add more prices as needed
            )
            
            productPrices.forEach { price ->
                database.productPriceDao().insert(price)
            }

            // Populate customers
            val customers = listOf(
                CustomerEntity(
                    firstname = "John",
                    lastname = "Doe",
                    contact = "09123456789",
                    customer_type = CustomerType.RETAIL,
                    address = "123 Main St",
                    city = "Sample City",
                    province = "Sample Province",
                    postal_code = "1234"
                ),
                CustomerEntity(
                    firstname = "Jane",
                    lastname = "Smith",
                    contact = "09987654321",
                    customer_type = CustomerType.WHOLESALE,
                    address = "456 Oak Ave",
                    city = "Another City",
                    province = "Another Province",
                    postal_code = "5678"
                )
                // Add more customers as needed
            )
            
            customers.forEach { customer ->
                database.customerDao().insertCustomer(customer)
            }

        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error during data initialization", e)
            throw e
        }
    }
} 