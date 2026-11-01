package com.prakash.pnotes

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prakash.pnotes.data.export.ExportRepository
import com.prakash.pnotes.data.local.database.PNotesDatabase
import com.prakash.pnotes.data.repository.NoteRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var db: PNotesDatabase
    private lateinit var repository: NoteRepository
    private lateinit var exportRepository: ExportRepository

    @Before
    fun setUp() {
        openDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportThenImportRestoresNotesAndChecklists() {
        runBlocking {
            val noteId = repository.createNote("Backup title", "Backup body")
            repository.addChecklistItem(noteId, "Milk", 0)
            repository.addChecklistItem(noteId, "Eggs", 1)
            val firstItem = repository.getChecklistSnapshot()[noteId]!!.minBy { it.position }
            repository.toggleChecklistItem(firstItem, true)

            val file = File(exportRepository.exportToJson())
            assertTrue(file.exists())

            // Fresh database to simulate restoring on another install.
            db.close()
            openDatabase()

            val result = exportRepository.importFromJson(Uri.fromFile(file))

            assertEquals(1, result.imported)
            assertEquals(0, result.skipped)

            val restored = repository.getAllNotesSnapshot().single()
            assertEquals("Backup title", restored.title)
            assertEquals("Backup body", restored.content)
            assertTrue(restored.hasChecklist)

            val items = repository.getChecklistSnapshot()[restored.id].orEmpty()
                .sortedBy { it.position }
            assertEquals(listOf("Milk", "Eggs"), items.map { it.text })
            assertTrue(items.first { it.text == "Milk" }.isDone)

            file.delete()
        }
    }

    @Test
    fun importSkipsWhenLocalNoteIsNewer() {
        runBlocking {
            val noteId = repository.createNote("Local", "before backup")
            val file = File(exportRepository.exportToJson())

            repository.updateNoteContent(noteId, "Local", "edited after backup")

            val result = exportRepository.importFromJson(Uri.fromFile(file))

            assertEquals(0, result.imported)
            assertEquals(1, result.skipped)
            assertEquals(
                "edited after backup",
                repository.getAllNotesSnapshot().single().content
            )

            file.delete()
        }
    }

    private fun openDatabase() {
        db = Room.inMemoryDatabaseBuilder(context, PNotesDatabase::class.java).build()
        repository = NoteRepository(db.noteDao(), db.checklistDao())
        exportRepository = ExportRepository(context, repository)
    }
}
