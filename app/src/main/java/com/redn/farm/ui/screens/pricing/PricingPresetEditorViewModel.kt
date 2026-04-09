package com.redn.farm.ui.screens.pricing

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redn.farm.data.local.entity.PricingPresetEntity
import com.redn.farm.data.local.session.SessionManager
import com.redn.farm.data.pricing.CategoryOverride
import com.redn.farm.data.pricing.ChannelConfig
import com.redn.farm.data.pricing.ChannelFee
import com.redn.farm.data.pricing.ChannelsConfiguration
import com.redn.farm.data.pricing.HaulingFeeItem
import com.redn.farm.data.pricing.PresetPreviewCalculator
import com.redn.farm.data.pricing.PricingPresetGson
import com.redn.farm.data.repository.PricingPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

data class PresetEditorForm(
    val presetName: String = "",
    val spoilageRateText: String = "0.25",
    val useDirectAdditionalPerKg: Boolean = false,
    val directAdditionalPerKgText: String = "",
    val haulingWeightKgText: String = "700",
    val haulingFees: List<HaulingFeeItem> = PricingPresetGson.defaultHaulingFees(),
    val channels: ChannelsConfiguration = PricingPresetGson.defaultChannelsConfiguration(),
    val categories: List<CategoryOverride> = emptyList()
)

@HiltViewModel
class PricingPresetEditorViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val repository: PricingPresetRepository
) : ViewModel() {

    private val sourcePresetId: String =
        when (val r = savedStateHandle.get<String>("sourcePresetId") ?: "new") {
            "new", "" -> ""
            else -> r
        }
    private val sessionManager = SessionManager(context)

    private val _form = MutableStateFlow(PresetEditorForm())
    val form: StateFlow<PresetEditorForm> = _form.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (sourcePresetId.isNotEmpty()) {
            viewModelScope.launch {
                repository.getPresetById(sourcePresetId)?.let { loadFromEntity(it) }
            }
        }
    }

    private fun loadFromEntity(e: PricingPresetEntity) {
        val fees = try {
            PricingPresetGson.haulingFeesFromJson(e.hauling_fees_json)
        } catch (_: Exception) {
            emptyList()
        }
        val channels = try {
            PricingPresetGson.channelsFromJson(e.channels_json)
        } catch (_: Exception) {
            PricingPresetGson.defaultChannelsConfiguration()
        }
        val cats = try {
            PricingPresetGson.categoriesFromJson(e.categories_json)
        } catch (_: Exception) {
            emptyList()
        }
        val derived = PresetPreviewCalculator.derivedAdditionalCostPerKg(
            e.hauling_weight_kg,
            fees
        )
        val useDirect = fees.isEmpty() || abs(e.additional_cost_per_kg - derived) > 0.01
        _form.value = PresetEditorForm(
            presetName = e.preset_name + " (copy)",
            spoilageRateText = e.spoilage_rate.toString(),
            useDirectAdditionalPerKg = useDirect,
            directAdditionalPerKgText = e.additional_cost_per_kg.toString(),
            haulingWeightKgText = e.hauling_weight_kg.toString(),
            haulingFees = if (fees.isNotEmpty()) fees else PricingPresetGson.defaultHaulingFees(),
            channels = channels,
            categories = cats
        )
    }

    fun updateForm(transform: (PresetEditorForm) -> PresetEditorForm) {
        _form.update(transform)
    }

    fun addHaulingFee() {
        _form.update { it.copy(haulingFees = it.haulingFees + HaulingFeeItem("fee", 0.0)) }
    }

    fun removeHaulingFee(index: Int) {
        _form.update {
            it.copy(haulingFees = it.haulingFees.filterIndexed { i, _ -> i != index })
        }
    }

    fun updateHaulingFee(index: Int, label: String, amount: Double) {
        _form.update { f ->
            val list = f.haulingFees.toMutableList()
            if (index in list.indices) list[index] = HaulingFeeItem(label, amount)
            f.copy(haulingFees = list)
        }
    }

    fun addCategory() {
        _form.update {
            it.copy(categories = it.categories + CategoryOverride("Category ${it.categories.size + 1}"))
        }
    }

    fun removeCategory(index: Int) {
        _form.update { it.copy(categories = it.categories.filterIndexed { i, _ -> i != index }) }
    }

    fun updateCategory(index: Int, block: (CategoryOverride) -> CategoryOverride) {
        _form.update { f ->
            val list = f.categories.toMutableList()
            if (index in list.indices) list[index] = block(list[index])
            f.copy(categories = list)
        }
    }

    fun updateChannelOnline(cfg: ChannelConfig) {
        _form.update { it.copy(channels = it.channels.copy(online = cfg)) }
    }

    fun updateChannelReseller(cfg: ChannelConfig) {
        _form.update { it.copy(channels = it.channels.copy(reseller = cfg)) }
    }

    fun updateChannelOffline(cfg: ChannelConfig) {
        _form.update { it.copy(channels = it.channels.copy(offline = cfg)) }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun save() {
        viewModelScope.launch {
            val f = _form.value
            val spoilage = f.spoilageRateText.toDoubleOrNull()
            if (spoilage == null || spoilage < 0 || spoilage >= 1.0) {
                _error.value = "Spoilage must be between 0 and 0.99"
                return@launch
            }
            val haulingW = f.haulingWeightKgText.toDoubleOrNull()
            if (haulingW == null || haulingW <= 0) {
                _error.value = "Hauling weight (kg) must be positive"
                return@launch
            }
            if (!channelsValid(f.channels)) {
                _error.value = "Each channel needs markup % OR margin % (not both, not neither)"
                return@launch
            }
            val additional = if (f.useDirectAdditionalPerKg) {
                val d = f.directAdditionalPerKgText.toDoubleOrNull()
                if (d == null || d < 0) {
                    _error.value = "Direct ₱/kg must be a non-negative number"
                    return@launch
                }
                d
            } else {
                PresetPreviewCalculator.derivedAdditionalCostPerKg(haulingW, f.haulingFees)
            }
            val name = f.presetName.trim().ifEmpty {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                "Preset ${fmt.format(Date())}"
            }
            val entity = PricingPresetEntity(
                preset_id = UUID.randomUUID().toString(),
                preset_name = name,
                saved_at = System.currentTimeMillis(),
                saved_by = sessionManager.getUsername() ?: "staff",
                is_active = false,
                activated_at = null,
                activated_by = null,
                spoilage_rate = spoilage,
                additional_cost_per_kg = additional,
                hauling_weight_kg = haulingW,
                hauling_fees_json = PricingPresetGson.haulingFeesToJson(f.haulingFees),
                channels_json = PricingPresetGson.channelsToJson(f.channels),
                categories_json = PricingPresetGson.categoriesToJson(f.categories)
            )
            try {
                repository.savePreset(entity)
                _saveMessage.value = "Saved inactive preset: ${entity.preset_name} (${entity.preset_id.take(8)}…)"
            } catch (e: Exception) {
                _error.value = e.message ?: "Save failed"
            }
        }
    }

    private fun channelsValid(c: ChannelsConfiguration): Boolean =
        listOf(c.online, c.reseller, c.offline).all { cfg ->
            val hasM = cfg.markupPercent != null
            val hasG = cfg.marginPercent != null
            hasM xor hasG
        }
}
