package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.RemittanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemittanceDao {
    @Query("SELECT * FROM remittances ORDER BY date DESC")
    fun getAllRemittances(): Flow<List<RemittanceEntity>>

    @Insert
    suspend fun insert(remittance: RemittanceEntity)

    @Update
    suspend fun update(remittance: RemittanceEntity)

    @Delete
    suspend fun delete(remittance: RemittanceEntity)

    @Query("DELETE FROM remittances")
    suspend fun truncate()

    // ─── EOD aggregation queries (Phase 2b) ──────────────────────────────────

    /**
     * Store-assistant remits only (`REMITTANCE` or legacy blank/null if any).
     * EOD-US-04 "Total remitted today".
     */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM remittances
        WHERE date BETWEEN :startMillis AND :endMillis
        AND (entry_type IS NULL OR entry_type = '' OR entry_type = 'REMITTANCE')
        """
    )
    suspend fun getSumRemittancesOnDate(startMillis: Long, endMillis: Long): Double

    /** Purchasing disbursements recorded the same window (informational / EOD other line). */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM remittances
        WHERE date BETWEEN :startMillis AND :endMillis AND entry_type = 'DISBURSEMENT'
        """
    )
    suspend fun getSumDisbursementsOnDate(startMillis: Long, endMillis: Long): Double
}