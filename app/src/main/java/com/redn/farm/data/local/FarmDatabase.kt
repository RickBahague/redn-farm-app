package com.redn.farm.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.redn.farm.data.local.dao.ProductDao
import com.redn.farm.data.local.dao.ProductPriceDao
import com.redn.farm.data.local.dao.CustomerDao
import com.redn.farm.data.local.dao.OrderDao
import com.redn.farm.data.local.dao.AcquisitionDao
import com.redn.farm.data.local.dao.RemittanceDao
import com.redn.farm.data.local.dao.EmployeeDao
import com.redn.farm.data.local.dao.EmployeePaymentDao
import com.redn.farm.data.local.dao.FarmOperationDao
import com.redn.farm.data.local.entity.ProductEntity
import com.redn.farm.data.local.entity.ProductPriceEntity
import com.redn.farm.data.local.entity.CustomerEntity
import com.redn.farm.data.local.entity.OrderEntity
import com.redn.farm.data.local.entity.OrderItemEntity
import com.redn.farm.data.local.entity.AcquisitionEntity
import com.redn.farm.data.local.entity.RemittanceEntity
import com.redn.farm.data.local.entity.EmployeeEntity
import com.redn.farm.data.local.entity.EmployeePaymentEntity
import com.redn.farm.data.local.entity.FarmOperationEntity
import com.redn.farm.data.local.util.Converters

@Database(
    entities = [
        ProductEntity::class, 
        ProductPriceEntity::class,
        CustomerEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        AcquisitionEntity::class,
        RemittanceEntity::class,
        EmployeeEntity::class,
        EmployeePaymentEntity::class,
        FarmOperationEntity::class
    ],
    version = 23,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FarmDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productPriceDao(): ProductPriceDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao
    abstract fun acquisitionDao(): AcquisitionDao
    abstract fun remittanceDao(): RemittanceDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun employeePaymentDao(): EmployeePaymentDao
    abstract fun farmOperationDao(): FarmOperationDao

    companion object {
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop the old table
                database.execSQL("DROP TABLE IF EXISTS customers")
                
                // Create the new table with all fields
                database.execSQL("""
                    CREATE TABLE customers (
                        customer_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        firstname TEXT NOT NULL,
                        lastname TEXT NOT NULL,
                        contact TEXT NOT NULL,
                        customer_type TEXT NOT NULL,
                        address TEXT NOT NULL,
                        city TEXT NOT NULL,
                        province TEXT NOT NULL,
                        postal_code TEXT NOT NULL,
                        date_created TEXT NOT NULL,
                        date_updated TEXT NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create orders table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS orders (
                        order_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customer_id INTEGER NOT NULL,
                        total_amount REAL NOT NULL,
                        order_date TEXT NOT NULL,
                        order_update_date TEXT NOT NULL,
                        FOREIGN KEY (customer_id) REFERENCES customers(customer_id) 
                        ON DELETE RESTRICT
                    )
                """)

                // Create order_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        order_id INTEGER NOT NULL,
                        product_id TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        price_per_unit REAL NOT NULL,
                        is_per_kg INTEGER NOT NULL,
                        total_price REAL NOT NULL,
                        FOREIGN KEY (order_id) REFERENCES orders(order_id) 
                        ON DELETE CASCADE,
                        FOREIGN KEY (product_id) REFERENCES products(product_id) 
                        ON DELETE RESTRICT
                    )
                """)
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop the old table if it exists
                database.execSQL("DROP TABLE IF EXISTS product_prices")
                
                // Create the new table with correct column names
                database.execSQL("""
                    CREATE TABLE product_prices (
                        price_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        product_id TEXT NOT NULL,
                        per_kg_price REAL,
                        per_piece_price REAL,
                        date_created TEXT NOT NULL,
                        FOREIGN KEY (product_id) REFERENCES products(product_id) 
                        ON DELETE CASCADE
                    )
                """)
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create temporary table with new schema
                database.execSQL("""
                    CREATE TABLE products_new (
                        product_id TEXT PRIMARY KEY NOT NULL,
                        product_name TEXT NOT NULL,
                        product_description TEXT NOT NULL,
                        unit_type TEXT NOT NULL DEFAULT 'piece',
                        is_active INTEGER NOT NULL DEFAULT 1
                    )
                """)

                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO products_new (product_id, product_name, product_description)
                    SELECT product_id, product_name, product_description FROM products
                """)

                // Drop old table
                database.execSQL("DROP TABLE products")

                // Rename new table to original name
                database.execSQL("ALTER TABLE products_new RENAME TO products")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add is_paid column to orders table
                database.execSQL("""
                    ALTER TABLE orders 
                    ADD COLUMN is_paid INTEGER NOT NULL DEFAULT 0
                """)
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Update existing REGULAR customers to RETAIL
                database.execSQL("""
                    UPDATE customers 
                    SET customer_type = 'RETAIL' 
                    WHERE customer_type = 'REGULAR'
                """)
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE customers ADD COLUMN customer_type TEXT NOT NULL DEFAULT 'RETAIL'"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS acquisitions (
                        acquisition_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        product_id TEXT NOT NULL,
                        product_name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        is_per_kg INTEGER NOT NULL,
                        total_amount REAL NOT NULL,
                        location TEXT NOT NULL,
                        date_acquired TEXT NOT NULL,
                        FOREIGN KEY (product_id) REFERENCES products(product_id) 
                        ON DELETE RESTRICT
                    )
                """)
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS remittances (
                        remittance_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        date INTEGER NOT NULL,
                        remarks TEXT NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add date_updated column with current timestamp as default
                database.execSQL("""
                    ALTER TABLE remittances 
                    ADD COLUMN date_updated INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
                """)
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS employees (
                        employee_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        firstname TEXT NOT NULL,
                        lastname TEXT NOT NULL,
                        contact TEXT NOT NULL,
                        date_created INTEGER NOT NULL,
                        date_updated INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a temporary table with the new schema
                database.execSQL("""
                    CREATE TABLE orders_new (
                        order_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customer_id INTEGER NOT NULL,
                        total_amount REAL NOT NULL,
                        order_date INTEGER NOT NULL,
                        order_update_date INTEGER NOT NULL,
                        is_paid INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (customer_id) REFERENCES customers(customer_id) 
                        ON DELETE RESTRICT
                    )
                """)

                // Copy data from old table to new table, converting dates to timestamps
                database.execSQL("""
                    INSERT INTO orders_new (order_id, customer_id, total_amount, order_date, order_update_date, is_paid)
                    SELECT 
                        order_id, 
                        customer_id, 
                        total_amount, 
                        strftime('%s', order_date) * 1000, 
                        strftime('%s', order_update_date) * 1000,
                        is_paid 
                    FROM orders
                """)

                // Drop old table and rename new table
                database.execSQL("DROP TABLE orders")
                database.execSQL("ALTER TABLE orders_new RENAME TO orders")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS employee_payments (
                        payment_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        employee_id INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        date_paid INTEGER NOT NULL,
                        signature TEXT NOT NULL,
                        received_date INTEGER,
                        FOREIGN KEY (employee_id) REFERENCES employees(employee_id) 
                        ON DELETE RESTRICT
                    )
                """)
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE employee_payments 
                    ADD COLUMN cash_advance_amount REAL DEFAULT NULL
                """)
                database.execSQL("""
                    ALTER TABLE employee_payments 
                    ADD COLUMN liquidated_amount REAL DEFAULT NULL
                """)
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a temporary table with the new schema
                database.execSQL("""
                    CREATE TABLE employee_payments_new (
                        payment_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        employee_id INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        cash_advance_amount REAL DEFAULT NULL,
                        liquidated_amount REAL DEFAULT NULL,
                        date_paid INTEGER NOT NULL,
                        signature TEXT NOT NULL,
                        received_date INTEGER,
                        FOREIGN KEY (employee_id) REFERENCES employees(employee_id) 
                        ON DELETE RESTRICT
                    )
                """)

                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO employee_payments_new (
                        payment_id, employee_id, amount, date_paid, signature, received_date
                    )
                    SELECT payment_id, employee_id, amount, date_paid, signature, received_date
                    FROM employee_payments
                """)

                // Drop old table and rename new table
                database.execSQL("DROP TABLE employee_payments")
                database.execSQL("ALTER TABLE employee_payments_new RENAME TO employee_payments")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the farm_operations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS farm_operations (
                        operation_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        operation_type TEXT NOT NULL,
                        operation_date INTEGER NOT NULL,
                        details TEXT NOT NULL,
                        area TEXT NOT NULL,
                        weather_condition TEXT NOT NULL,
                        personnel TEXT NOT NULL,
                        date_created INTEGER NOT NULL,
                        date_updated INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to farm_operations table
                database.execSQL("""
                    ALTER TABLE farm_operations 
                    ADD COLUMN product_id TEXT DEFAULT NULL 
                    REFERENCES products(product_id) ON DELETE SET NULL
                """)
                
                database.execSQL("""
                    ALTER TABLE farm_operations 
                    ADD COLUMN product_name TEXT NOT NULL DEFAULT ''
                """)
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create index on product_id
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_farm_operations_product_id ON farm_operations(product_id)"
                )
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop existing table and recreate with correct schema
                database.execSQL("DROP TABLE IF EXISTS acquisitions")

                // Create new table with correct schema
                database.execSQL("""
                    CREATE TABLE acquisitions (
                        acquisition_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        product_id TEXT NOT NULL,
                        product_name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        price_per_unit REAL NOT NULL,
                        total_amount REAL NOT NULL,
                        is_per_kg INTEGER NOT NULL,
                        date_acquired INTEGER NOT NULL,
                        location TEXT NOT NULL,
                        FOREIGN KEY (product_id) 
                            REFERENCES products(product_id) 
                            ON DELETE RESTRICT
                    )
                """)

                // Create any necessary indices
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_acquisitions_product_id 
                    ON acquisitions(product_id)
                """)
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE orders 
                    ADD COLUMN is_delivered INTEGER NOT NULL DEFAULT 0
                """)
            }
        }

        @Volatile
        private var INSTANCE: FarmDatabase? = null

        fun getDatabase(context: Context): FarmDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    // Try to get existing database
                    INSTANCE?.let { return it }

                    Log.d("FarmDatabase", "Creating new database instance")
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        FarmDatabase::class.java,
                        "farm_database"
                    )
                        .addCallback(DatabaseInitializer(context.applicationContext).callback)
                        .addMigrations(
                            MIGRATION_4_5, 
                            MIGRATION_5_6, 
                            MIGRATION_6_7, 
                            MIGRATION_7_8,
                            MIGRATION_8_9,
                            MIGRATION_9_10,
                            MIGRATION_1_2,
                            MIGRATION_10_11,
                            MIGRATION_11_12,
                            MIGRATION_12_13,
                            MIGRATION_13_14,
                            MIGRATION_14_15,
                            MIGRATION_15_16,
                            MIGRATION_16_17,
                            MIGRATION_17_18,
                            MIGRATION_18_19,
                            MIGRATION_19_20,
                            MIGRATION_20_21,
                            MIGRATION_21_22,
                            MIGRATION_22_23
                        )
                        .fallbackToDestructiveMigration()
                        .allowMainThreadQueries()  // Only for debugging, remove in production
                        .build()

                    // Verify database creation
                    instance.openHelper.writableDatabase
                    Log.d("FarmDatabase", "Database created successfully")

                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("FarmDatabase", "Error creating database", e)
                    throw e
                }
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
} 