package com.redn.farm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "day_close_audit",
    foreignKeys = [
        ForeignKey(
            entity = DayCloseEntity::class,
            parentColumns = ["close_id"],
            childColumns = ["close_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("close_id")
    ]
)
data class DayCloseAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val audit_id: Int = 0,

    val close_id: Int,

    /**
     * Action code — one of: OPEN, SAVE_DRAFT, ENTER_COUNTS, FINALIZE,
     * UNFINALIZE, EDIT_CASH, EDIT_NOTES.
     */
    val action: String,

    val username: String,

    val at_millis: Long = System.currentTimeMillis(),

    /** Optional free-text context for the action. */
    val note: String? = null,
)
