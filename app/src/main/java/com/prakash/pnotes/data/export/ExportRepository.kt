package com.prakash.pnotes.data.export

import android.content.Context
import android.net.Uri
import com.prakash.pnotes.data.repository.NoteRepository
import com.prakash.pnotes.domain.model.ChecklistItem
import com.prakash.pnotes.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Summary of a backup restore. */
data class ImportResult(
    val imported: Int,
    val skipped: Int
)

class ExportRepository(
    private val context: Context,
    private val noteRepository: NoteRepository
) {

    /**
     * Writes every note (active, archived and trashed) plus its checklist items
     * to a JSON file and returns the absolute path.
     */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val notes = noteRepository.getAllNotesSnapshot()
        val itemsByNote = noteRepository.getChecklistSnapshot()

        val notesArray = JSONArray()
        for (note in notes) {
            notesArray.put(note.toJson(itemsByNote[note.id].orEmpty()))
        }

        val export = JSONObject().apply {
            put("notes", notesArray)
            put("exportedAt", System.currentTimeMillis())
        }

        val filename = "P-Notes-Backup-${
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        }.json"
        val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
        backupDir.mkdirs()
        val file = File(backupDir, filename)
        file.writeText(export.toString(2))
        file.absolutePath
    }

    /**
     * Restores notes from a file produced by [exportToJson]. Notes are matched by
     * id; a local note is only replaced when the backup copy is newer. Notes that
     * already exist with the same or newer timestamp are counted as skipped.
     */
    suspend fun importFromJson(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Could not open the selected file")

        val notesArray = parseNotes(text)
        val existing = noteRepository.getAllNotesSnapshot()
            .associateBy { it.id }
            .toMutableMap()

        var imported = 0
        var skipped = 0

        for (index in 0 until notesArray.length()) {
            val json = notesArray.optJSONObject(index)
            if (json == null) {
                skipped++
                continue
            }
            val note = json.toNote()
            if (note == null) {
                skipped++
                continue
            }

            val current = existing[note.id]
            if (current != null && current.updatedAt >= note.updatedAt) {
                skipped++
                continue
            }

            val items = json.optJSONArray("checklistItems").toChecklistItems(note.id)
            noteRepository.upsertNote(note.copy(hasChecklist = note.hasChecklist || items.isNotEmpty()))
            noteRepository.replaceChecklistItems(note.id, items)
            existing[note.id] = note
            imported++
        }

        ImportResult(imported = imported, skipped = skipped)
    }

    private fun parseNotes(text: String): JSONArray {
        val trimmed = text.trim()
        return if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val root = JSONObject(trimmed)
            root.optJSONArray("notes")
                ?: throw IllegalArgumentException("This file is not a P-Notes backup")
        }
    }

    private fun JSONObject.toNote(): Note? {
        val id = optString("id").takeIf { it.isNotBlank() }
        val title = optString("title")
        val content = optString("content")
        if (id == null && title.isBlank() && content.isBlank()) return null

        val now = System.currentTimeMillis()
        return Note(
            id = id ?: UUID.randomUUID().toString(),
            title = title,
            content = content,
            createdAt = optLong("createdAt", now),
            updatedAt = optLong("updatedAt", now),
            color = optInt("color", 0),
            isPinned = optBoolean("isPinned", false),
            isArchived = optBoolean("isArchived", false),
            isTrashed = optBoolean("isTrashed", false),
            hasChecklist = optBoolean("hasChecklist", false),
            labels = optJSONArray("labels").toStringList()
        )
    }

    private fun JSONArray?.toChecklistItems(noteId: String): List<ChecklistItem> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            val obj = optJSONObject(index) ?: return@mapNotNull null
            val text = obj.optString("text")
            if (text.isBlank()) return@mapNotNull null
            ChecklistItem(
                id = obj.optString("id").takeIf { it.isNotBlank() }
                    ?: UUID.randomUUID().toString(),
                noteId = noteId,
                text = text,
                isDone = obj.optBoolean("isDone", false),
                position = obj.optInt("position", index)
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optString(index).takeIf { it.isNotBlank() }
        }
    }

    private fun Note.toJson(items: List<ChecklistItem>): JSONObject {
        val itemsArray = JSONArray()
        for (item in items) {
            itemsArray.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("noteId", item.noteId)
                    put("text", item.text)
                    put("isDone", item.isDone)
                    put("position", item.position)
                }
            )
        }
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("content", content)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
            put("color", color)
            put("isPinned", isPinned)
            put("isArchived", isArchived)
            put("isTrashed", isTrashed)
            put("hasChecklist", hasChecklist)
            put("labels", JSONArray(labels))
            put("checklistItems", itemsArray)
        }
    }
}
