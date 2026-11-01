package com.prakash.pnotes.data.repository

import com.prakash.pnotes.data.local.dao.ChecklistDao
import com.prakash.pnotes.data.local.dao.NoteDao
import com.prakash.pnotes.data.local.entity.ChecklistItemEntity
import com.prakash.pnotes.data.local.entity.NoteEntity
import com.prakash.pnotes.domain.model.ChecklistItem
import com.prakash.pnotes.domain.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val checklistDao: ChecklistDao
) {
    /**
     * Application-lifetime scope for writes that must survive the screen that
     * triggered them (e.g. saving on back navigation).
     */
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getActiveNotes(): Flow<List<Note>> = noteDao.getActiveNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getTrashedNotes(): Flow<List<Note>> = noteDao.getTrashedNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getNoteByIdFlow(id: String): Flow<Note?> = noteDao.getNoteByIdFlow(id).map { it?.toDomain() }

    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)?.toDomain()

    suspend fun createNote(title: String = "", content: String = "", color: Int = 0): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = NoteEntity(
            id = id,
            title = title,
            content = content,
            createdAt = now,
            updatedAt = now,
            color = color
        )
        noteDao.insertNote(entity)
        return id
    }

    suspend fun updateNote(note: Note) {
        val entity = note.toEntity()
        noteDao.updateNote(entity.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateNoteContent(id: String, title: String, content: String) {
        val existing = noteDao.getNoteById(id) ?: return
        noteDao.updateNote(
            existing.copy(
                title = title,
                content = content,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Saves the latest editor text without depending on the caller's coroutine scope. */
    fun persistContentAsync(id: String?, title: String, content: String) {
        writeScope.launch {
            if (id != null) {
                updateNoteContent(id, title, content)
            } else if (title.isNotBlank() || content.isNotBlank()) {
                createNote(title, content)
            }
        }
    }

    /** Archives a note even if the editor screen is closing immediately. */
    fun archiveNoteAsync(id: String) {
        writeScope.launch { archiveNote(id) }
    }

    /** Trashes a note even if the editor screen is closing immediately. */
    fun moveToTrashAsync(id: String) {
        writeScope.launch { moveToTrash(id) }
    }

    suspend fun setPinned(id: String, pinned: Boolean) = noteDao.setPinned(id, pinned)
    suspend fun archiveNote(id: String) = noteDao.archiveNote(id)
    suspend fun unarchiveNote(id: String) = noteDao.unarchiveNote(id)
    suspend fun moveToTrash(id: String) = noteDao.moveToTrash(id)
    suspend fun restoreFromTrash(id: String) = noteDao.restoreFromTrash(id)
    suspend fun setHasChecklist(id: String, hasChecklist: Boolean) =
        noteDao.setHasChecklist(id, hasChecklist)

    suspend fun deleteNotePermanently(note: Note) {
        checklistDao.deleteItemsForNote(note.id)
        noteDao.deleteNote(note.toEntity())
    }

    suspend fun emptyTrash() {
        checklistDao.deleteItemsForTrashedNotes()
        noteDao.emptyTrash()
    }

    /** Snapshot of every note (active, archived and trashed) for backup/sync. */
    suspend fun getAllNotesSnapshot(): List<Note> = noteDao.getAllNotesOnce().map { it.toDomain() }

    /** All checklist items grouped by note id, for backup/sync. */
    suspend fun getChecklistSnapshot(): Map<String, List<ChecklistItem>> =
        checklistDao.getAllItemsOnce().map { it.toDomain() }.groupBy { it.noteId }

    /** Inserts or replaces a note, used when merging notes from the cloud. */
    suspend fun upsertNote(note: Note) = noteDao.insertNote(note.toEntity())

    /** Replaces every checklist row for a note, used when restoring a backup. */
    suspend fun replaceChecklistItems(noteId: String, items: List<ChecklistItem>) {
        checklistDao.deleteItemsForNote(noteId)
        items.forEach { checklistDao.insertItem(it.toEntity()) }
    }

    fun getChecklistItems(noteId: String): Flow<List<ChecklistItem>> = 
        checklistDao.getItemsForNote(noteId).map { items -> items.map { it.toDomain() } }

    suspend fun addChecklistItem(noteId: String, text: String, position: Int) {
        val item = ChecklistItemEntity(
            id = UUID.randomUUID().toString(),
            noteId = noteId,
            text = text,
            position = position
        )
        checklistDao.insertItem(item)
        noteDao.setHasChecklist(noteId, true)
    }

    suspend fun toggleChecklistItem(item: ChecklistItem, done: Boolean) {
        checklistDao.updateItem(item.toEntity().copy(isDone = done))
    }

    suspend fun deleteChecklistItem(id: String) = checklistDao.deleteItem(id)
}

private fun NoteEntity.toDomain(): Note {
    val labels = try {
        val arr = JSONArray(labelsJson)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) { emptyList() }
    return Note(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        color = color,
        isPinned = isPinned,
        isArchived = isArchived,
        isTrashed = isTrashed,
        hasChecklist = hasChecklist,
        labels = labels
    )
}

private fun Note.toEntity(): NoteEntity {
    val arr = JSONArray(labels)
    return NoteEntity(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        color = color,
        isPinned = isPinned,
        isArchived = isArchived,
        isTrashed = isTrashed,
        hasChecklist = hasChecklist,
        labelsJson = arr.toString()
    )
}

private fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(id, noteId, text, isDone, position)
private fun ChecklistItem.toEntity(): ChecklistItemEntity = ChecklistItemEntity(id, noteId, text, isDone, position)
