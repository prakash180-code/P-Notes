package com.prakash.pnotes.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prakash.pnotes.data.local.dao.ChecklistDao
import com.prakash.pnotes.data.local.dao.NoteDao
import com.prakash.pnotes.data.local.entity.ChecklistItemEntity
import com.prakash.pnotes.data.local.entity.NoteEntity

@Database(
    entities = [NoteEntity::class, ChecklistItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun checklistDao(): ChecklistDao
}
