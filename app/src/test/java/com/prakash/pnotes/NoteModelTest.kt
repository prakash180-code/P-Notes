package com.prakash.pnotes

import org.junit.Test
import org.junit.Assert.*

class NoteModelTest {
    @Test
    fun note_creation_has_defaults() {
        val note = com.prakash.pnotes.domain.model.Note(
            id = "1",
            title = "Test",
            content = "Content",
            createdAt = 123L,
            updatedAt = 123L
        )
        assertEquals("1", note.id)
        assertFalse(note.isPinned)
        assertFalse(note.isArchived)
        assertFalse(note.isTrashed)
    }

    @Test
    fun note_copy_preserves_fields() {
        val original = com.prakash.pnotes.domain.model.Note(
            id = "1",
            title = "Original",
            content = "Content",
            createdAt = 100L,
            updatedAt = 100L,
            isPinned = true,
            color = 0xFF0000
        )
        val copied = original.copy(title = "Updated", updatedAt = 200L)
        assertEquals("Updated", copied.title)
        assertEquals(200L, copied.updatedAt)
        assertTrue(copied.isPinned)
        assertEquals(0xFF0000, copied.color)
        assertEquals("1", copied.id)
    }

    @Test
    fun checklist_item_defaults() {
        val item = com.prakash.pnotes.domain.model.ChecklistItem(
            id = "i1",
            noteId = "n1",
            text = "Buy milk"
        )
        assertFalse(item.isDone)
        assertEquals(0, item.position)
    }
}
