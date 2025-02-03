package com.redn.farm.di

import android.content.Context
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.dao.*
import com.redn.farm.data.repository.FarmOperationRepository
import com.redn.farm.data.repository.ProductRepository
import com.redn.farm.data.repository.AcquisitionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideFarmOperationRepository(
        @ApplicationContext context: Context,
        database: FarmDatabase,
        farmOperationDao: FarmOperationDao
    ): FarmOperationRepository {
        return FarmOperationRepository(context, database, farmOperationDao)
    }

    @Provides
    @Singleton
    fun provideProductRepository(
        productDao: ProductDao,
        productPriceDao: ProductPriceDao
    ): ProductRepository {
        return ProductRepository(productDao, productPriceDao)
    }

    @Provides
    @Singleton
    fun provideAcquisitionRepository(
        acquisitionDao: AcquisitionDao
    ): AcquisitionRepository {
        return AcquisitionRepository(acquisitionDao)
    }
} 