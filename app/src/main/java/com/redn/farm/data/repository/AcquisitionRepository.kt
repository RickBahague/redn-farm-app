package com.redn.farm.data.repository

import com.redn.farm.data.local.dao.AcquisitionDao
import com.redn.farm.data.local.dao.ProductDao
import com.redn.farm.data.local.entity.AcquisitionEntity
import com.redn.farm.data.local.entity.PricingPresetEntity
import com.redn.farm.data.model.Acquisition
import com.redn.farm.data.pricing.ChannelsConfiguration
import com.redn.farm.data.pricing.PricingPresetGson
import com.redn.farm.data.pricing.SrpCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

sealed class AcquisitionSaveOutcome {
    data object Success : AcquisitionSaveOutcome()
    data class ValidationError(val message: String) : AcquisitionSaveOutcome()
    data class SavedWithoutActivePreset(val message: String) : AcquisitionSaveOutcome()
}

/** Live preview for acquisition dialog (active preset + product category). */
sealed class AcquisitionDraftPricingPreview {
    data object NoActivePreset : AcquisitionDraftPricingPreview()
    data class Invalid(val message: String) : AcquisitionDraftPricingPreview()
    data class Ok(val presetName: String, val output: SrpCalculator.Output) : AcquisitionDraftPricingPreview()
}

@Singleton
class AcquisitionRepository @Inject constructor(
    private val acquisitionDao: AcquisitionDao,
    private val pricingPresetRepository: PricingPresetRepository,
    private val productDao: ProductDao
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

    /** One row per product with the currently active SRP lot (INV-US-06). */
    fun observeAllActiveSrps(): Flow<List<Acquisition>> {
        return acquisitionDao.getAllActiveSrps().map { entities ->
            entities.map { it.toAcquisition() }
        }
    }

    suspend fun getAcquisitionById(id: Int): Acquisition? =
        acquisitionDao.getById(id)?.toAcquisition()

    suspend fun getActiveSrpForProduct(productId: String): Acquisition? =
        acquisitionDao.getActiveSrpForProduct(productId)?.toAcquisition()

    /**
     * Computes SRPs from the **active** preset and product category without persisting.
     * Used for live preview while editing an acquisition draft.
     */
    suspend fun previewDraftPricing(acquisition: Acquisition): AcquisitionDraftPricingPreview {
        val preset = pricingPresetRepository.getActivePresetOnce()
            ?: return AcquisitionDraftPricingPreview.NoActivePreset
        val category = productDao.getProductById(acquisition.product_id)?.category
        return when (val run = runSrpForAcquisitionAndPreset(acquisition, preset, category)) {
            is SrpRun.Error -> AcquisitionDraftPricingPreview.Invalid(run.message)
            is SrpRun.Ok -> AcquisitionDraftPricingPreview.Ok(presetName = preset.preset_name, output = run.output)
        }
    }

    /** Sample / seed data — no preset required. */
    suspend fun insertPlain(acquisition: Acquisition) {
        acquisitionDao.insert(acquisition.withoutPricing().toEntityForSave(isInsert = true))
    }

    suspend fun insertWithPricing(acquisition: Acquisition): AcquisitionSaveOutcome {
        val preset = pricingPresetRepository.getActivePresetOnce()
        if (preset == null) {
            acquisitionDao.insert(acquisition.withoutPricing().toEntityForSave(isInsert = true))
            return AcquisitionSaveOutcome.SavedWithoutActivePreset("No active preset — SRPs not computed")
        }
        val category = productDao.getProductById(acquisition.product_id)?.category
        return computeInsertOrFail(acquisition, preset, category)
    }

    suspend fun updateWithPricing(user: Acquisition): AcquisitionSaveOutcome {
        val row = acquisitionDao.getById(user.acquisition_id)
            ?: return AcquisitionSaveOutcome.ValidationError("Acquisition not found")
        val old = row.toAcquisition()

        val costChanged = user.quantity != old.quantity ||
            user.price_per_unit != old.price_per_unit ||
            user.piece_count != old.piece_count ||
            abs(user.total_amount - old.total_amount) > 0.001 ||
            user.is_per_kg != old.is_per_kg

        if (!costChanged) {
            val merged = user.copy(
                created_at = old.created_at,
                preset_ref = old.preset_ref,
                spoilage_rate = old.spoilage_rate,
                additional_cost_per_kg = old.additional_cost_per_kg,
                hauling_weight_kg = old.hauling_weight_kg,
                hauling_fees_json = old.hauling_fees_json,
                channels_snapshot_json = old.channels_snapshot_json,
                srp_online_per_kg = old.srp_online_per_kg,
                srp_reseller_per_kg = old.srp_reseller_per_kg,
                srp_offline_per_kg = old.srp_offline_per_kg,
                srp_online_500g = old.srp_online_500g,
                srp_online_250g = old.srp_online_250g,
                srp_online_100g = old.srp_online_100g,
                srp_reseller_500g = old.srp_reseller_500g,
                srp_reseller_250g = old.srp_reseller_250g,
                srp_reseller_100g = old.srp_reseller_100g,
                srp_offline_500g = old.srp_offline_500g,
                srp_offline_250g = old.srp_offline_250g,
                srp_offline_100g = old.srp_offline_100g,
                srp_online_per_piece = old.srp_online_per_piece,
                srp_reseller_per_piece = old.srp_reseller_per_piece,
                srp_offline_per_piece = old.srp_offline_per_piece
            )
            acquisitionDao.update(merged.toEntityForSave(isInsert = false))
            return AcquisitionSaveOutcome.Success
        }

        if (
            old.channels_snapshot_json != null &&
            old.spoilage_rate != null &&
            old.additional_cost_per_kg != null
        ) {
            return try {
                val channels = PricingPresetGson.channelsFromJson(old.channels_snapshot_json!!)
                val qKg = SrpCalculator.bulkQuantityKg(user.quantity, user.is_per_kg, user.piece_count)
                    ?: return AcquisitionSaveOutcome.ValidationError(
                        "Enter piece count (pieces per kg) for per-piece acquisitions."
                    )
                when (
                    val r = SrpCalculator.compute(
                        SrpCalculator.Input(
                            bulkCost = user.total_amount,
                            bulkQuantityKg = qKg,
                            spoilageRate = old.spoilage_rate!!,
                            additionalCostPerKg = old.additional_cost_per_kg!!,
                            channels = channels,
                            pieceCount = user.piece_count
                        )
                    )
                ) {
                    is SrpCalculator.Result.Error ->
                        AcquisitionSaveOutcome.ValidationError(r.message)
                    is SrpCalculator.Result.Ok -> {
                        val merged = user.withSnapshotAndOutput(old, r.output)
                        acquisitionDao.update(merged.toEntityForSave(isInsert = false))
                        AcquisitionSaveOutcome.Success
                    }
                }
            } catch (e: Exception) {
                AcquisitionSaveOutcome.ValidationError(
                    "Could not recompute SRP from stored snapshot: ${e.message ?: "invalid data"}"
                )
            }
        }

        val preset = pricingPresetRepository.getActivePresetOnce()
        if (preset != null) {
            val category = productDao.getProductById(user.product_id)?.category
            return when (val out = computeMerged(user.copy(created_at = old.created_at), preset, category)) {
                is ComputeResult.Error -> AcquisitionSaveOutcome.ValidationError(out.message)
                is ComputeResult.Ok -> {
                    acquisitionDao.update(out.acquisition.toEntityForSave(isInsert = false))
                    AcquisitionSaveOutcome.Success
                }
            }
        }

        val merged = user.copy(
            created_at = old.created_at,
            preset_ref = old.preset_ref,
            spoilage_rate = old.spoilage_rate,
            additional_cost_per_kg = old.additional_cost_per_kg,
            hauling_weight_kg = old.hauling_weight_kg,
            hauling_fees_json = old.hauling_fees_json,
            channels_snapshot_json = old.channels_snapshot_json,
            srp_online_per_kg = old.srp_online_per_kg,
            srp_reseller_per_kg = old.srp_reseller_per_kg,
            srp_offline_per_kg = old.srp_offline_per_kg,
            srp_online_500g = old.srp_online_500g,
            srp_online_250g = old.srp_online_250g,
            srp_online_100g = old.srp_online_100g,
            srp_reseller_500g = old.srp_reseller_500g,
            srp_reseller_250g = old.srp_reseller_250g,
            srp_reseller_100g = old.srp_reseller_100g,
            srp_offline_500g = old.srp_offline_500g,
            srp_offline_250g = old.srp_offline_250g,
            srp_offline_100g = old.srp_offline_100g,
            srp_online_per_piece = old.srp_online_per_piece,
            srp_reseller_per_piece = old.srp_reseller_per_piece,
            srp_offline_per_piece = old.srp_offline_per_piece
        )
        acquisitionDao.update(merged.toEntityForSave(isInsert = false))
        return AcquisitionSaveOutcome.Success
    }

    suspend fun deleteAcquisition(acquisition: Acquisition) {
        acquisitionDao.delete(acquisition.toEntityForSave(isInsert = false))
    }

    suspend fun truncate() {
        acquisitionDao.truncate()
    }

    private sealed class ComputeResult {
        data class Ok(val acquisition: Acquisition) : ComputeResult()
        data class Error(val message: String) : ComputeResult()
    }

    private sealed class SrpRun {
        data class Error(val message: String) : SrpRun()
        data class Ok(
            val spoilage: Double,
            val additional: Double,
            val channels: ChannelsConfiguration,
            val output: SrpCalculator.Output
        ) : SrpRun()
    }

    private fun runSrpForAcquisitionAndPreset(
        acquisition: Acquisition,
        preset: PricingPresetEntity,
        category: String?
    ): SrpRun {
        val (spoilage, additional, channels) = resolveCategoryAndChannels(preset, category)
        val qKg = SrpCalculator.bulkQuantityKg(acquisition.quantity, acquisition.is_per_kg, acquisition.piece_count)
            ?: return SrpRun.Error("Enter piece count (pieces per kg) for per-piece acquisitions.")
        return when (
            val r = SrpCalculator.compute(
                SrpCalculator.Input(
                    bulkCost = acquisition.total_amount,
                    bulkQuantityKg = qKg,
                    spoilageRate = spoilage,
                    additionalCostPerKg = additional,
                    channels = channels,
                    pieceCount = acquisition.piece_count
                )
            )
        ) {
            is SrpCalculator.Result.Error -> SrpRun.Error(r.message)
            is SrpCalculator.Result.Ok -> SrpRun.Ok(spoilage, additional, channels, r.output)
        }
    }

    private suspend fun computeInsertOrFail(
        acquisition: Acquisition,
        preset: PricingPresetEntity,
        category: String?
    ): AcquisitionSaveOutcome {
        return when (val out = computeMerged(acquisition, preset, category)) {
            is ComputeResult.Error -> AcquisitionSaveOutcome.ValidationError(out.message)
            is ComputeResult.Ok -> {
                acquisitionDao.insert(out.acquisition.toEntityForSave(isInsert = true))
                AcquisitionSaveOutcome.Success
            }
        }
    }

    private fun computeMerged(
        acquisition: Acquisition,
        preset: PricingPresetEntity,
        category: String?
    ): ComputeResult {
        return when (val run = runSrpForAcquisitionAndPreset(acquisition, preset, category)) {
            is SrpRun.Error -> ComputeResult.Error(run.message)
            is SrpRun.Ok ->
                ComputeResult.Ok(
                    acquisition.withComputedPricing(
                        preset.preset_id,
                        run.spoilage,
                        run.additional,
                        preset,
                        run.channels,
                        run.output
                    )
                )
        }
    }

    private fun resolveCategoryAndChannels(
        preset: PricingPresetEntity,
        productCategory: String?
    ): Triple<Double, Double, ChannelsConfiguration> {
        val cats = PricingPresetGson.categoriesFromJson(preset.categories_json)
        val match = productCategory?.trim()?.takeIf { it.isNotEmpty() }?.let { pc ->
            cats.firstOrNull { it.name.equals(pc, ignoreCase = true) }
        }
        val spoilage = match?.spoilageRate ?: preset.spoilage_rate
        val additional = match?.additionalCostPerKg ?: preset.additional_cost_per_kg
        val channels = PricingPresetGson.channelsFromJson(preset.channels_json)
        return Triple(spoilage, additional, channels)
    }

    private fun Acquisition.withComputedPricing(
        presetId: String,
        spoilage: Double,
        additional: Double,
        preset: PricingPresetEntity,
        channels: ChannelsConfiguration,
        out: SrpCalculator.Output
    ) = copy(
        preset_ref = presetId,
        spoilage_rate = spoilage,
        additional_cost_per_kg = additional,
        hauling_weight_kg = preset.hauling_weight_kg,
        hauling_fees_json = preset.hauling_fees_json,
        channels_snapshot_json = PricingPresetGson.channelsToJson(channels),
        srp_online_per_kg = out.srpOnlinePerKg,
        srp_reseller_per_kg = out.srpResellerPerKg,
        srp_offline_per_kg = out.srpOfflinePerKg,
        srp_online_500g = out.srpOnline500g,
        srp_online_250g = out.srpOnline250g,
        srp_online_100g = out.srpOnline100g,
        srp_reseller_500g = out.srpReseller500g,
        srp_reseller_250g = out.srpReseller250g,
        srp_reseller_100g = out.srpReseller100g,
        srp_offline_500g = out.srpOffline500g,
        srp_offline_250g = out.srpOffline250g,
        srp_offline_100g = out.srpOffline100g,
        srp_online_per_piece = out.srpOnlinePerPiece,
        srp_reseller_per_piece = out.srpResellerPerPiece,
        srp_offline_per_piece = out.srpOfflinePerPiece
    )

    private fun Acquisition.withSnapshotAndOutput(old: Acquisition, out: SrpCalculator.Output) = copy(
        created_at = old.created_at,
        preset_ref = old.preset_ref,
        spoilage_rate = old.spoilage_rate,
        additional_cost_per_kg = old.additional_cost_per_kg,
        hauling_weight_kg = old.hauling_weight_kg,
        hauling_fees_json = old.hauling_fees_json,
        channels_snapshot_json = old.channels_snapshot_json,
        srp_online_per_kg = out.srpOnlinePerKg,
        srp_reseller_per_kg = out.srpResellerPerKg,
        srp_offline_per_kg = out.srpOfflinePerKg,
        srp_online_500g = out.srpOnline500g,
        srp_online_250g = out.srpOnline250g,
        srp_online_100g = out.srpOnline100g,
        srp_reseller_500g = out.srpReseller500g,
        srp_reseller_250g = out.srpReseller250g,
        srp_reseller_100g = out.srpReseller100g,
        srp_offline_500g = out.srpOffline500g,
        srp_offline_250g = out.srpOffline250g,
        srp_offline_100g = out.srpOffline100g,
        srp_online_per_piece = out.srpOnlinePerPiece,
        srp_reseller_per_piece = out.srpResellerPerPiece,
        srp_offline_per_piece = out.srpOfflinePerPiece
    )

    private fun Acquisition.withoutPricing() = copy(
        preset_ref = null,
        spoilage_rate = null,
        additional_cost_per_kg = null,
        hauling_weight_kg = null,
        hauling_fees_json = null,
        channels_snapshot_json = null,
        srp_online_per_kg = null,
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

    private fun AcquisitionEntity.toAcquisition() = Acquisition(
        acquisition_id = acquisition_id,
        product_id = product_id,
        product_name = product_name,
        quantity = quantity,
        price_per_unit = price_per_unit,
        total_amount = total_amount,
        is_per_kg = is_per_kg,
        piece_count = piece_count,
        date_acquired = date_acquired,
        created_at = created_at,
        location = location,
        preset_ref = preset_ref,
        spoilage_rate = spoilage_rate,
        additional_cost_per_kg = additional_cost_per_kg,
        hauling_weight_kg = hauling_weight_kg,
        hauling_fees_json = hauling_fees_json,
        channels_snapshot_json = channels_snapshot_json,
        srp_online_per_kg = srp_online_per_kg,
        srp_reseller_per_kg = srp_reseller_per_kg,
        srp_offline_per_kg = srp_offline_per_kg,
        srp_online_500g = srp_online_500g,
        srp_online_250g = srp_online_250g,
        srp_online_100g = srp_online_100g,
        srp_reseller_500g = srp_reseller_500g,
        srp_reseller_250g = srp_reseller_250g,
        srp_reseller_100g = srp_reseller_100g,
        srp_offline_500g = srp_offline_500g,
        srp_offline_250g = srp_offline_250g,
        srp_offline_100g = srp_offline_100g,
        srp_online_per_piece = srp_online_per_piece,
        srp_reseller_per_piece = srp_reseller_per_piece,
        srp_offline_per_piece = srp_offline_per_piece
    )

    private fun Acquisition.toEntityForSave(isInsert: Boolean) = AcquisitionEntity(
        acquisition_id = acquisition_id,
        product_id = product_id,
        product_name = product_name,
        quantity = quantity,
        price_per_unit = price_per_unit,
        total_amount = total_amount,
        is_per_kg = is_per_kg,
        piece_count = piece_count,
        date_acquired = date_acquired,
        created_at = if (isInsert) System.currentTimeMillis() else created_at,
        location = location,
        preset_ref = preset_ref,
        spoilage_rate = spoilage_rate,
        additional_cost_per_kg = additional_cost_per_kg,
        hauling_weight_kg = hauling_weight_kg,
        hauling_fees_json = hauling_fees_json,
        channels_snapshot_json = channels_snapshot_json,
        srp_online_per_kg = srp_online_per_kg,
        srp_reseller_per_kg = srp_reseller_per_kg,
        srp_offline_per_kg = srp_offline_per_kg,
        srp_online_500g = srp_online_500g,
        srp_online_250g = srp_online_250g,
        srp_online_100g = srp_online_100g,
        srp_reseller_500g = srp_reseller_500g,
        srp_reseller_250g = srp_reseller_250g,
        srp_reseller_100g = srp_reseller_100g,
        srp_offline_500g = srp_offline_500g,
        srp_offline_250g = srp_offline_250g,
        srp_offline_100g = srp_offline_100g,
        srp_online_per_piece = srp_online_per_piece,
        srp_reseller_per_piece = srp_reseller_per_piece,
        srp_offline_per_piece = srp_offline_per_piece
    )
}
