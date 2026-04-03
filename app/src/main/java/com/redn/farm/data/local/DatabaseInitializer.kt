package com.redn.farm.data.local

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.redn.farm.data.model.CustomerList
import com.redn.farm.data.model.CustomerType
import com.redn.farm.data.local.entity.CustomerEntity
import com.redn.farm.data.local.entity.ProductEntity
import com.redn.farm.data.local.entity.ProductPriceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

class DatabaseInitializer(private val context: Context) {
    private val gson: Gson = GsonBuilder()
        .create()
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val zone: ZoneId = ZoneId.systemDefault()

    private data class SeedProducts(val products: List<SeedProduct>)
    private data class SeedProduct(
        val product_id: String,
        val product_name: String,
        val product_description: String? = null
    ) {
        fun description(): String = product_description ?: ""
    }

    private data class SeedProductPrices(val product_prices: List<SeedProductPrice>)
    private data class SeedProductPrice(
        val product_id: String,
        val per_kg_price: Double?,
        val per_piece_price: Double?,
        val valid_date: String
    )

    private fun readAssetText(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private fun parseSeedLocalDateToMillis(dateText: String): Long =
        try {
            LocalDate.parse(dateText).atStartOfDay(zone).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            System.currentTimeMillis()
        }

    private fun parseSeedDateTimeToMillis(dateText: String, fallback: Long): Long =
        runCatching {
            LocalDateTime.parse(dateText).atZone(zone).toInstant().toEpochMilli()
        }.recoverCatching {
            LocalDate.parse(dateText).atStartOfDay(zone).toInstant().toEpochMilli()
        }.getOrDefault(fallback)

    val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("DatabaseInitializer", "Database creation started")

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

    private suspend fun populateDatabase() {
        val database = FarmDatabase.getDatabase(context)

        try {
            val seedProducts = gson.fromJson(
                readAssetText("data/products.json"),
                SeedProducts::class.java
            )
            seedProducts.products.forEach { p ->
                database.productDao().insertProduct(
                    ProductEntity(
                        product_id = p.product_id,
                        product_name = p.product_name,
                        product_description = p.description(),
                        unit_type = "kg",
                        category = null,
                        default_piece_count = null,
                        is_active = true
                    )
                )
            }

            val seedPrices = gson.fromJson(
                readAssetText("data/product_prices.json"),
                SeedProductPrices::class.java
            )
            seedPrices.product_prices.forEach { price ->
                val createdAtMillis = parseSeedLocalDateToMillis(price.valid_date)
                database.productPriceDao().insert(
                    ProductPriceEntity(
                        product_id = price.product_id,
                        per_kg_price = price.per_kg_price,
                        per_piece_price = price.per_piece_price,
                        discounted_per_kg_price = null,
                        discounted_per_piece_price = null,
                        date_created = createdAtMillis
                    )
                )
            }

            val seedCustomers = gson.fromJson(
                readAssetText("data/customers.json"),
                CustomerList::class.java
            )
            seedCustomers.customers.forEach { c ->
                val ct = runCatching { CustomerType.valueOf(c.customer_type.uppercase()) }
                    .getOrDefault(CustomerType.RETAIL)
                val now = System.currentTimeMillis()
                val createdAt = parseSeedDateTimeToMillis(c.date_created, now)
                val updatedAt = parseSeedDateTimeToMillis(c.date_updated, createdAt)
                database.customerDao().insertCustomer(
                    CustomerEntity(
                        firstname = c.firstname,
                        lastname = c.lastname,
                        contact = c.contact,
                        customer_type = ct,
                        address = c.address,
                        city = c.city,
                        province = c.province,
                        postal_code = c.postal_code,
                        date_created = createdAt,
                        date_updated = updatedAt
                    )
                )
            }

        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error during data initialization", e)
            throw e
        }
    }
}
