package com.prakash.pnotes.ui.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pnotes.data.repository.NoteRepository
import com.prakash.pnotes.domain.model.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ArchiveViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {
    val notes: StateFlow<List<Note>> = repository.getArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
