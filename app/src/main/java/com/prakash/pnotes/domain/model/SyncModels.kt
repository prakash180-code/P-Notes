package com.prakash.pnotes.domain.model

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class SyncedNote(
    val noteId: String,
    val lastSyncedAt: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.IDLE
)

data class SyncResult(
    val success: Boolean,
    val message: String = ""
)
