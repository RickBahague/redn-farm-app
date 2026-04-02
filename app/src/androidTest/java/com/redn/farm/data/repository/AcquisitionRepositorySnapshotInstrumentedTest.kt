package com.redn.farm.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.dao.AcquisitionDao
import com.redn.farm.data.local.entity.AcquisitionEntity
import com.redn.farm.data.local.entity.ProductEntity
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.model.AcquisitionLocation
import com.redn.farm.data.pricing.PricingPresetGson
import com.redn.farm.data.pricing.SrpCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Snapshot-based SRP recompute on [AcquisitionRepository.updateWithPricing] (Phase 3 follow-up).
 *
 * Run: `./gradlew connectedAndroidTest --tests com.redn.farm.data.repository.AcquisitionRepositorySnapshotInstrumentedTest`
 */
@RunWith(AndroidJUnit4::class)
class AcquisitionRepositorySnapshotInstrumentedTest {

    private lateinit var db: FarmDatabase
    private lateinit var acquisitionDao: AcquisitionDao
    private lateinit var repo: AcquisitionRepository

    private val channelsJson: String =
        PricingPresetGson.channelsToJson(PricingPresetGson.defaultChannelsConfiguration())

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FarmDatabase::class.java
        ).allowMainThreadQueries().build()
        acquisitionDao = db.acquisitionDao()
        val presetRepo = PricingPresetRepository(
            db.pricingPresetDao(),
            db.presetActivationLogDao()
        )
        repo = AcquisitionRepository(acquisitionDao, presetRepo, db.productDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun insertProduct() {
        db.productDao().insertProduct(
            ProductEntity(
                product_id = "p1",
                product_name = "Test",
                product_description = "",
                unit_type = "kg",
                category = null,
                default_piece_count = null,
                is_active = true
            )
        )
    }

    private fun baseRow(
        total: Double = 1000.0,
        qty: Double = 50.0,
        onlineSrp: Double? = 111.0
    ) = AcquisitionEntity(
        acquisition_id = 0,
        product_id = "p1",
        product_name = "Test",
        quantity = qty,
        price_per_unit = total / qty,
        total_amount = total,
        is_per_kg = true,
        piece_count = null,
        date_acquired = 1L,
        created_at = 100L,
        location = AcquisitionLocation.FARM,
        preset_ref = "preset1",
        spoilage_rate = 0.1,
        additional_cost_per_kg = 5.0,
        hauling_weight_kg = null,
        hauling_fees_json = null,
        channels_snapshot_json = channelsJson,
        srp_online_per_kg = onlineSrp,
        srp_reseller_per_kg = null,
        srp_offline_per_kg = null,
        srp_online_500g = null,
        srp_online_250g = null,
        srp_online_100g = null,
        srp_reseller_500g = null,
        srp_reseller_250g = null,
        srp_reseller_100g = null,
        srp_offline_500g = null,
        srp_offline_250g = null,
        srp_offline_100g = null,
        srp_online_per_piece = null,
        srp_reseller_per_piece = null,
        srp_offline_per_piece = null
    )

    @Test
    fun updateWithPricing_costChange_recomputesFromSnapshot() = runTest {
        insertProduct()
        acquisitionDao.insert(baseRow(total = 1000.0, onlineSrp = 111.0))
        val id = acquisitionDao.getAllAcquisitions().first().first().acquisition_id

        val user = Acquisition(
            acquisition_id = id,
            product_id = "p1",
            product_name = "Test",
            quantity = 50.0,
            price_per_unit = 40.0,
            total_amount = 2000.0,
            is_per_kg = true,
            piece_count = null,
            date_acquired = 1L,
            created_at = 100L,
            location = AcquisitionLocation.FARM
        )
        assertTrue(repo.updateWithPricing(user) is AcquisitionSaveOutcome.Success)

        val expected = SrpCalculator.compute(
            SrpCalculator.Input(
                bulkCost = 2000.0,
                bulkQuantityKg = 50.0,
                spoilageRate = 0.1,
                additionalCostPerKg = 5.0,
                channels = PricingPresetGson.channelsFromJson(channelsJson),
                pieceCount = null
            )
        ) as SrpCalculator.Result.Ok

        val updated = acquisitionDao.getById(id)!!
        assertEquals(2000.0, updated.total_amount, 0.001)
        assertEquals(expected.output.srpOnlinePerKg, updated.srp_online_per_kg!!, 0.02)
    }

    @Test
    fun updateWithPricing_metadataOnly_keepsSrps() = runTest {
        insertProduct()
        acquisitionDao.insert(baseRow(total = 1000.0, onlineSrp = 999.0))
        val id = acquisitionDao.getAllAcquisitions().first().first().acquisition_id

        val user = Acquisition(
            acquisition_id = id,
            product_id = "p1",
            product_name = "Test",
            quantity = 50.0,
            price_per_unit = 20.0,
            total_amount = 1000.0,
            is_per_kg = true,
            piece_count = null,
            date_acquired = 1L,
            created_at = 100L,
            location = AcquisitionLocation.MARKET
        )
        assertTrue(repo.updateWithPricing(user) is AcquisitionSaveOutcome.Success)

        val updated = acquisitionDao.getById(id)!!
        assertEquals(999.0, updated.srp_online_per_kg!!, 0.001)
        assertEquals(AcquisitionLocation.MARKET, updated.location)
    }
}
