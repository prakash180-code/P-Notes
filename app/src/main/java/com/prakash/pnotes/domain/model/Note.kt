package com.prakash.pnotes.domain.model

import java.time.Instant

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val color: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val hasChecklist: Boolean = false,
    val labels: List<String> = emptyList()
)
