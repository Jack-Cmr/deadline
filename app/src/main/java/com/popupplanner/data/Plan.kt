package com.popupplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single, one-shot plan. [triggerAt] is an absolute epoch-millisecond value,
 * which makes the stored deadline independent of how it is formatted in the UI.
 */
@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val triggerAt: Long
)
