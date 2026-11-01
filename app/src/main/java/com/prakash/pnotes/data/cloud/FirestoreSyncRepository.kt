package com.prakash.pnotes.data.cloud

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.prakash.pnotes.data.repository.NoteRepository
import com.prakash.pnotes.domain.model.ChecklistItem
import com.prakash.pnotes.domain.model.Note
import com.prakash.pnotes.domain.model.SyncResult
import com.prakash.pnotes.domain.model.SyncStatus
import com.prakash.pnotes.domain.repository.CloudSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firestore-backed sync. Notes are stored under users/{userId}/notes/{noteId},
 * with their checklist items embedded in the same document. The newest document
 * (by updatedAt) wins when merging.
 */
class FirestoreSyncRepository(
    private val userId: String,
    private val noteRepository: NoteRepository
) : CloudSyncRepository {

    private val db: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        null
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    override suspend fun sync(): SyncResult {
        val firestore = db
            ?: return SyncResult(success = false, message = "Firebase is not configured")

        _syncStatus.value = SyncStatus.SYNCING
        return try {
            val localNotes = noteRepository.getAllNotesSnapshot()
            val itemsByNote = noteRepository.getChecklistSnapshot()
            val collection = firestore.collection("users").document(userId).collection("notes")

            // Upload in chunks to stay within Firestore's 500-write batch limit.
            localNotes.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { note ->
                    batch.set(
                        collection.document(note.id),
                        note.toCloudMap(itemsByNote[note.id].orEmpty())
                    )
                }
                batch.commit().await()
            }

            val remoteSnapshot = collection.get().await()
            val localById = localNotes.associateBy { it.id }
            var downloadedNotes = 0
            var downloadedItems = 0

            remoteSnapshot.documents.forEach { document ->
                val remoteUpdatedAt = document.getLong("updatedAt") ?: 0L
                val existing = localById[document.id]
                if (existing == null || remoteUpdatedAt > existing.updatedAt) {
                    noteRepository.upsertNote(document.toNote())
                    // Older clients did not upload checklists: leave local items alone.
                    if (document.contains("checklistItems")) {
                        val items = document.toChecklistItems()
                        noteRepository.replaceChecklistItems(document.id, items)
                        downloadedItems += items.size
                    }
                    downloadedNotes++
                }
            }

            _syncStatus.value = SyncStatus.SUCCESS
            SyncResult(
                success = true,
                message = "Uploaded ${localNotes.size} note(s), " +
                    "downloaded $downloadedNotes note(s) and $downloadedItems checklist item(s)"
            )
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            SyncResult(success = false, message = e.message ?: "Sync failed")
        }
    }

    private fun Note.toCloudMap(items: List<ChecklistItem>): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "content" to content,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "color" to color,
        "isPinned" to isPinned,
        "isArchived" to isArchived,
        "isTrashed" to isTrashed,
        "hasChecklist" to hasChecklist,
        "labels" to labels,
        "checklistItems" to items.map { it.toCloudMap() }
    )

    private fun ChecklistItem.toCloudMap(): Map<String, Any> = mapOf(
        "id" to id,
        "text" to text,
        "isDone" to isDone,
        "position" to position
    )

    private fun DocumentSnapshot.toNote(): Note = Note(
        id = id,
        title = getString("title") ?: "",
        content = getString("content") ?: "",
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
        color = (getLong("color") ?: 0L).toInt(),
        isPinned = getBoolean("isPinned") ?: false,
        isArchived = getBoolean("isArchived") ?: false,
        isTrashed = getBoolean("isTrashed") ?: false,
        hasChecklist = getBoolean("hasChecklist") ?: false,
        labels = (get("labels") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    )

    private fun DocumentSnapshot.toChecklistItems(): List<ChecklistItem> {
        val rawItems = get("checklistItems") as? List<*> ?: return emptyList()
        return rawItems.mapNotNull { rawItem ->
            val map = rawItem as? Map<*, *> ?: return@mapNotNull null
            val text = map["text"] as? String ?: return@mapNotNull null
            if (text.isBlank()) return@mapNotNull null
            ChecklistItem(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                noteId = id,
                text = text,
                isDone = map["isDone"] as? Boolean ?: false,
                position = (map["position"] as? Number)?.toInt() ?: 0
            )
        }.sortedBy { it.position }
    }

    private companion object {
        const val BATCH_LIMIT = 400
    }
}
