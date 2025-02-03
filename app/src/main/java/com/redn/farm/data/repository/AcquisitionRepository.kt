package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.AcquisitionDao
import com.redn.farm.data.local.entity.AcquisitionEntity
import com.redn.farm.data.model.Acquisition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AcquisitionRepository(
    private val acquisitionDao: AcquisitionDao
) {
    fun getAllAcquisitions(): Flow<List<Acquisition>> {
        return acquisitionDao.getAllAcquisitions().map { entities ->
            entities.map { it.toAcquisition() }
        }
    }

    fun getAcquisitionsForProduct(productId: String): Flow<List<Acquisition>> {
        return acquisitionDao.getAcquisitionsForProduct(productId).map { entities ->
            entities.map { it.toAcquisition() }
        }
    }

    suspend fun addAcquisition(acquisition: Acquisition) {
        acquisitionDao.insert(acquisition.toEntity())
    }

    suspend fun updateAcquisition(acquisition: Acquisition) {
        acquisitionDao.update(acquisition.toEntity())
    }

    suspend fun deleteAcquisition(acquisition: Acquisition) {
        acquisitionDao.delete(acquisition.toEntity())
    }

    suspend fun truncate() {
        acquisitionDao.truncate()
    }

    private fun AcquisitionEntity.toAcquisition() = Acquisition(
        acquisition_id = acquisition_id,
        product_id = product_id,
        product_name = product_name,
        quantity = quantity,
        price_per_unit = price_per_unit,
        total_amount = total_amount,
        is_per_kg = is_per_kg,
        date_acquired = date_acquired,
        location = location
    )

    private fun Acquisition.toEntity() = AcquisitionEntity(
        acquisition_id = acquisition_id,
        product_id = product_id,
        product_name = product_name,
        quantity = quantity,
        price_per_unit = price_per_unit,
        total_amount = total_amount,
        is_per_kg = is_per_kg,
        date_acquired = date_acquired,
        location = location
    )
} 