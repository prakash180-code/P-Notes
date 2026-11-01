package com.prakash.pnotes.domain.model

data class ChecklistItem(
    val id: String,
    val noteId: String,
    val text: String,
    val isDone: Boolean = false,
    val position: Int = 0
)
