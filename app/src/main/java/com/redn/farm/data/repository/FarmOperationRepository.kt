package com.redn.farm.data.repository

import android.content.Context
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.dao.FarmOperationDao
import com.redn.farm.data.local.entity.FarmOperationEntity
import com.redn.farm.data.local.util.DatabaseExporter
import com.redn.farm.data.model.FarmOperation
import com.redn.farm.data.model.FarmOperationType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FarmOperationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FarmDatabase,
    private val farmOperationDao: FarmOperationDao
) {
    fun getAllOperations(): Flow<List<FarmOperation>> {
        return farmOperationDao.getAllOperations().map { entities ->
            entities.map { it.toOperation() }
        }
    }

    suspend fun addOperation(operation: FarmOperation) {
        farmOperationDao.insert(operation.toEntity())
    }

    suspend fun updateOperation(operation: FarmOperation) {
        farmOperationDao.update(operation.toEntity().copy(
            date_updated = System.currentTimeMillis()
        ))
    }

    suspend fun deleteOperation(operation: FarmOperation) {
        farmOperationDao.delete(operation.toEntity())
    }

    suspend fun truncate() {
        farmOperationDao.truncate()
    }

    fun getOperationsInDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<FarmOperation>> {
        val startMillis = startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return farmOperationDao.getOperationsInDateRange(startMillis, endMillis).map { entities ->
            entities.map { it.toOperation() }
        }
    }

    suspend fun exportDatabase(): String {
        val exporter = DatabaseExporter(context, database)
        return exporter.exportDatabase()
    }

    private fun FarmOperationEntity.toOperation() = FarmOperation(
        operation_id = operation_id,
        operation_type = operation_type,
        operation_date = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(operation_date),
            ZoneId.systemDefault()
        ),
        details = details,
        area = area,
        weather_condition = weather_condition,
        personnel = personnel,
        date_created = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(date_created),
            ZoneId.systemDefault()
        ),
        date_updated = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(date_updated),
            ZoneId.systemDefault()
        ),
        product_id = product_id,
        product_name = product_name
    )

    private fun FarmOperation.toEntity() = FarmOperationEntity(
        operation_id = operation_id,
        operation_type = operation_type,
        operation_date = operation_date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        details = details,
        area = area,
        weather_condition = weather_condition,
        personnel = personnel,
        date_created = date_created.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        date_updated = date_updated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        product_id = product_id,
        product_name = product_name
    )
} 