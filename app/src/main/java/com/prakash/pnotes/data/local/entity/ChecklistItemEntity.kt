package com.prakash.pnotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val text: String,
    val isDone: Boolean = false,
    val position: Int = 0
)
