package com.redn.farm.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.redn.farm.data.local.FarmDatabase
import com.redn.farm.data.local.entity.RemittanceEntity
import com.redn.farm.data.model.RemittanceEntryType
import com.redn.farm.data.repository.RemittanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for:
 *   BUG-03 — RemittanceRepository.toRemittance() drops date_updated when mapping
 *             entity → model. Every read returns System.currentTimeMillis() instead
 *             of the stored value.
 *
 * Run with: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.redn.farm.data.local.dao.RemittanceDaoTest
 */
@RunWith(AndroidJUnit4::class)
class RemittanceDaoTest {

    private lateinit var db: FarmDatabase
    private lateinit var repository: RemittanceRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FarmDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemittanceRepository(db.remittanceDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // BUG-03 — date_updated dropped in repository mapping
    // -------------------------------------------------------------------------

    @Test
    fun remittance_dateUpdated_isPreservedThroughRepository() = runTest {
        val specificTimestamp = 1_700_000_000_000L // 2023-11-14 — a fixed known value

        db.remittanceDao().insert(
            RemittanceEntity(
                amount = 1000.0,
                date = specificTimestamp,
                remarks = "test",
                date_updated = specificTimestamp
            )
        )

        val loaded = repository.getAllRemittances().first().first()

        assertEquals(
            "BUG-03: date_updated must equal the stored value, not System.currentTimeMillis(). " +
            "Fix: pass date_updated = date_updated in RemittanceRepository.toRemittance()",
            specificTimestamp,
            loaded.date_updated
        )
    }

    @Test
    fun remittance_date_isPreserved() = runTest {
        val timestamp = 1_700_000_000_000L

        db.remittanceDao().insert(
            RemittanceEntity(amount = 500.0, date = timestamp, remarks = "payment")
        )

        val loaded = repository.getAllRemittances().first().first()
        assertEquals(timestamp, loaded.date)
        assertEquals(500.0, loaded.amount, 0.001)
        assertEquals("payment", loaded.remarks)
        assertEquals(RemittanceEntryType.REMITTANCE, loaded.entry_type)
    }

    @Test
    fun remittance_disbursement_roundTrip() = runTest {
        db.remittanceDao().insert(
            RemittanceEntity(
                amount = 250.0,
                date = 1_700_000_000_000L,
                remarks = "Float",
                entry_type = RemittanceEntryType.DISBURSEMENT,
            )
        )
        val loaded = repository.getAllRemittances().first().first()
        assertEquals(RemittanceEntryType.DISBURSEMENT, loaded.entry_type)
    }

    @Test
    fun remittance_updatePreservesAllFields() = runTest {
        val originalTimestamp = 1_700_000_000_000L
        db.remittanceDao().insert(
            RemittanceEntity(
                remittance_id = 1,
                amount = 100.0,
                date = originalTimestamp,
                remarks = "original",
                date_updated = originalTimestamp
            )
        )

        val stored = repository.getAllRemittances().first().first()
        val updated = stored.copy(amount = 200.0, remarks = "updated")
        repository.updateRemittance(updated)

        val reloaded = repository.getAllRemittances().first().first()
        assertEquals(200.0, reloaded.amount, 0.001)
        assertEquals("updated", reloaded.remarks)
    }
}
