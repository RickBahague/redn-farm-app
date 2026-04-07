package com.redn.farm.data.local.dao

import androidx.room.*
import com.redn.farm.data.local.entity.AcquisitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcquisitionDao {

    @Query("SELECT * FROM acquisitions ORDER BY date_acquired DESC, created_at DESC")
    fun getAllAcquisitions(): Flow<List<AcquisitionEntity>>

    @Query("SELECT * FROM acquisitions WHERE product_id = :productId ORDER BY date_acquired DESC, created_at DESC")
    fun getAcquisitionsForProduct(productId: String): Flow<List<AcquisitionEntity>>

    @Query("SELECT * FROM acquisitions WHERE acquisition_id = :id LIMIT 1")
    suspend fun getById(id: Int): AcquisitionEntity?

    /**
     * Returns the acquisition whose SRPs are currently "active" for [productId]:
     * most recent by date_acquired, tiebroken by created_at (DB insert time). (INV-US-06)
     */
    @Query("""
        SELECT * FROM acquisitions
        WHERE product_id = :productId
        ORDER BY date_acquired DESC, created_at DESC
        LIMIT 1
    """)
    suspend fun getActiveSrpForProduct(productId: String): AcquisitionEntity?

    /**
     * Active SRP for all products — one row per product_id, latest acquisition wins. (INV-US-06)
     */
    @Query("""
        SELECT a.* FROM acquisitions a
        INNER JOIN (
            SELECT product_id, MAX(date_acquired) AS max_date
            FROM acquisitions
            GROUP BY product_id
        ) latest ON a.product_id = latest.product_id AND a.date_acquired = latest.max_date
        WHERE a.created_at = (
            SELECT MAX(a2.created_at) FROM acquisitions a2
            WHERE a2.product_id = a.product_id AND a2.date_acquired = a.date_acquired
        )
    """)
    fun getAllActiveSrps(): Flow<List<AcquisitionEntity>>

    @Insert
    suspend fun insert(acquisition: AcquisitionEntity)

    @Update
    suspend fun update(acquisition: AcquisitionEntity)

    @Delete
    suspend fun delete(acquisition: AcquisitionEntity)

    @Query("DELETE FROM acquisitions")
    suspend fun truncate()

    // ─── EOD aggregation queries (Phase 2b) ──────────────────────────────────

    /**
     * Total acquired quantity (in native units) and total cost per product, all time.
     * Used by DayCloseRepository for WAC and running stock ledger (EOD-US-03 / EOD-US-07).
     *
     * Note (D1): per-piece acquisitions use quantity/piece_count conversion in the repository;
     * the raw quantity column is returned as-is here.
     */
    @Query("""
        SELECT
            product_id,
            SUM(quantity)      AS total_qty,
            SUM(total_amount)  AS total_cost,
            MAX(piece_count)   AS max_piece_count,
            MAX(is_per_kg)     AS is_per_kg_flag
        FROM acquisitions
        GROUP BY product_id
    """)
    suspend fun getTotalAcquiredByProduct(): List<ProductAcquisitionSummary>

    /**
     * All acquisition rows for a product, ordered oldest-first — used by InventoryFifoAllocator.
     */
    @Query("""
        SELECT * FROM acquisitions
        WHERE product_id = :productId
        ORDER BY date_acquired ASC, created_at ASC
    """)
    suspend fun getAcquisitionLotsForProduct(productId: String): List<AcquisitionEntity>

    /**
     * All acquisition rows for all products, ordered oldest-first — for full FIFO pass.
     */
    @Query("""
        SELECT * FROM acquisitions
        ORDER BY product_id, date_acquired ASC, created_at ASC
    """)
    suspend fun getAllAcquisitionLotsOldestFirst(): List<AcquisitionEntity>
}

// EOD result type

data class ProductAcquisitionSummary(
    val product_id: String,
    val total_qty: Double,
    val total_cost: Double,
    val max_piece_count: Double?,
    val is_per_kg_flag: Int,  // Room maps Boolean to Int in aggregates
)
