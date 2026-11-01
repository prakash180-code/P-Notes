package com.prakash.pnotes.domain.repository

import com.prakash.pnotes.domain.model.SyncResult
import com.prakash.pnotes.domain.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

interface CloudSyncRepository {
    val syncStatus: StateFlow<SyncStatus>

    /** Uploads local notes and merges newer remote notes into local storage. */
    suspend fun sync(): SyncResult
}
