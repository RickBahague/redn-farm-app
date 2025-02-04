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
        RemittanceEntity::class
    ],
    version = 1
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

    companion object {
        const val DATABASE_NAME = "farm_database"
        
        @Volatile
        private var INSTANCE: FarmDatabase? = null

        fun getDatabase(context: Context): FarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FarmDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabaseFile(context: Context) = context.getDatabasePath(DATABASE_NAME)

        fun doesDatabaseExist(context: Context): Boolean {
            return getDatabaseFile(context).exists()
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
} 