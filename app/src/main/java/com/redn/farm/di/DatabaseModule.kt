package com.redn.farm.di

import android.content.Context
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): FarmDatabase {
        return FarmDatabase.getDatabase(context)
    }

    @Provides
    fun provideFarmOperationDao(database: FarmDatabase): FarmOperationDao {
        return database.farmOperationDao()
    }

    @Provides
    fun provideProductDao(database: FarmDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    fun provideProductPriceDao(database: FarmDatabase): ProductPriceDao {
        return database.productPriceDao()
    }

    @Provides
    fun provideCustomerDao(database: FarmDatabase): CustomerDao {
        return database.customerDao()
    }

    @Provides
    fun provideOrderDao(database: FarmDatabase): OrderDao {
        return database.orderDao()
    }

    @Provides
    fun provideAcquisitionDao(database: FarmDatabase): AcquisitionDao {
        return database.acquisitionDao()
    }

    @Provides
    fun provideRemittanceDao(database: FarmDatabase): RemittanceDao {
        return database.remittanceDao()
    }

    @Provides
    fun provideEmployeeDao(database: FarmDatabase): EmployeeDao {
        return database.employeeDao()
    }

    @Provides
    fun provideEmployeePaymentDao(database: FarmDatabase): EmployeePaymentDao {
        return database.employeePaymentDao()
    }
} 