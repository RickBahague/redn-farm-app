package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.RemittanceDao
import com.redn.farm.data.local.entity.RemittanceEntity
import com.redn.farm.data.model.Remittance
import com.redn.farm.data.model.RemittanceEntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemittanceRepository @Inject constructor(
    private val remittanceDao: RemittanceDao
) {
    fun getAllRemittances(): Flow<List<Remittance>> {
        return remittanceDao.getAllRemittances().map { entities ->
            entities.map { it.toRemittance() }
        }
    }

    suspend fun addRemittance(remittance: Remittance) {
        remittanceDao.insert(remittance.toEntity())
    }

    suspend fun updateRemittance(remittance: Remittance) {
        remittanceDao.update(remittance.toEntity())
    }

    suspend fun deleteRemittance(remittance: Remittance) {
        remittanceDao.delete(remittance.toEntity())
    }

    suspend fun truncate() {
        remittanceDao.truncate()
    }

    private fun RemittanceEntity.toRemittance() = Remittance(
        remittance_id = remittance_id,
        amount = amount,
        date = date,
        remarks = remarks,
        date_updated = date_updated,
        entry_type = RemittanceEntryType.normalize(entry_type),
    )

    private fun Remittance.toEntity() = RemittanceEntity(
        remittance_id = remittance_id,
        amount = amount,
        date = date,
        remarks = remarks,
        date_updated = date_updated,
        entry_type = RemittanceEntryType.normalize(entry_type),
    )
} 