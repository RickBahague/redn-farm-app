package com.redn.farm.data.model

/** Epic 8 — stored on [Remittance.entry_type] / [com.redn.farm.data.local.entity.RemittanceEntity.entry_type]. */
object RemittanceEntryType {
    const val REMITTANCE = "REMITTANCE"
    const val DISBURSEMENT = "DISBURSEMENT"

    fun normalize(raw: String?): String =
        if (raw.isNullOrBlank() || raw == REMITTANCE) REMITTANCE
        else if (raw == DISBURSEMENT) DISBURSEMENT
        else REMITTANCE

    fun isDisbursement(raw: String?) = normalize(raw) == DISBURSEMENT

    fun label(raw: String?): String =
        if (isDisbursement(raw)) "Disbursement" else "Remittance"
}
