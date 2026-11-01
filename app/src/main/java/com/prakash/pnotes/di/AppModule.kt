package com.prakash.pnotes.di

import android.content.Context
import androidx.room.Room
import com.prakash.pnotes.data.local.database.PNotesDatabase
import com.prakash.pnotes.data.repository.NoteRepository

object AppModule {
    @Volatile
    private var database: PNotesDatabase? = null

    @Volatile
    private var repository: NoteRepository? = null

    fun getDatabase(context: Context): PNotesDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                PNotesDatabase::class.java,
                "pnotes.db"
            ).build().also { database = it }
        }
    }

    fun getRepository(context: Context): NoteRepository {
        return repository ?: synchronized(this) {
            val db = getDatabase(context)
            repository ?: NoteRepository(db.noteDao(), db.checklistDao()).also { repository = it }
        }
    }
}
