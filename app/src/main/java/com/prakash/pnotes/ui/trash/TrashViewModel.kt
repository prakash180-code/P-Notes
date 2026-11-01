package com.prakash.pnotes.ui.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pnotes.data.repository.NoteRepository
import com.prakash.pnotes.domain.model.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {
    val notes: StateFlow<List<Note>> = repository.getTrashedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(id: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }
}
