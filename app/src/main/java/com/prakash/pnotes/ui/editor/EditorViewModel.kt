package com.prakash.pnotes.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pnotes.data.repository.NoteRepository
import com.prakash.pnotes.domain.model.ChecklistItem
import com.prakash.pnotes.domain.model.Note
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val title: String = "",
    val content: String = "",
    val note: Note? = null,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val isLoading: Boolean = false
) {
    val checklistEnabled: Boolean
        get() = note?.hasChecklist == true || checklistItems.isNotEmpty()
}

class EditorViewModel(
    application: Application,
    private val repository: NoteRepository,
    noteId: String?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        EditorUiState(isLoading = noteId != null && noteId.isNotBlank() && noteId != "new")
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var currentId: String? = noteId?.takeIf { it.isNotBlank() && it != "new" }
    private var saveJob: Job? = null
    private var observeJob: Job? = null
    private var initialLoadDone = false

    init {
        currentId?.let(::observeNote)
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
        scheduleSave()
    }

    fun onContentChange(value: String) {
        _uiState.update { it.copy(content = value) }
        scheduleSave()
    }

    /** Cancels the debounce and writes immediately. Safe to call while leaving the screen. */
    fun flush() {
        saveJob?.cancel()
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return
        repository.persistContentAsync(currentId, state.title, state.content)
    }

    fun togglePin() {
        val note = _uiState.value.note ?: return
        viewModelScope.launch {
            repository.setPinned(note.id, !note.isPinned)
        }
    }

    fun archive() {
        flush()
        currentId?.let { repository.archiveNoteAsync(it) }
    }

    fun moveToTrash() {
        flush()
        currentId?.let { repository.moveToTrashAsync(it) }
    }

    fun toggleChecklist() {
        viewModelScope.launch {
            val id = ensureNoteId(forceCreate = true) ?: return@launch
            val enabled = _uiState.value.note?.hasChecklist == true
            repository.setHasChecklist(id, !enabled)
        }
    }

    fun addChecklistItem(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val id = ensureNoteId(forceCreate = true) ?: return@launch
            repository.addChecklistItem(id, text, _uiState.value.checklistItems.size)
        }
    }

    fun toggleChecklistItem(item: ChecklistItem, done: Boolean) {
        viewModelScope.launch {
            repository.toggleChecklistItem(item, done)
        }
    }

    fun deleteChecklistItem(item: ChecklistItem) {
        viewModelScope.launch {
            repository.deleteChecklistItem(item.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        flush()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DELAY_MS)
            saveNow()
        }
    }

    private suspend fun saveNow() {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return
        val id = currentId
        if (id == null) {
            val newId = repository.createNote(state.title, state.content)
            currentId = newId
            initialLoadDone = true
            observeNote(newId)
        } else {
            repository.updateNoteContent(id, state.title, state.content)
        }
    }

    private suspend fun ensureNoteId(forceCreate: Boolean): String? {
        currentId?.let { return it }
        val state = _uiState.value
        if (!forceCreate && state.title.isBlank() && state.content.isBlank()) return null
        val newId = repository.createNote(state.title, state.content)
        currentId = newId
        initialLoadDone = true
        observeNote(newId)
        return newId
    }

    /**
     * Observes the stored note and its checklist. The local title/content are
     * only seeded once, so database emissions (including our own saves) never
     * overwrite what the user is currently typing.
     */
    private fun observeNote(id: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                repository.getNoteByIdFlow(id),
                repository.getChecklistItems(id)
            ) { note, items -> note to items }
                .collect { (note, items) ->
                    if (note == null) {
                        // The note was deleted elsewhere: start a fresh note on the next edit.
                        currentId = null
                        initialLoadDone = false
                        _uiState.update { it.copy(note = null, isLoading = false) }
                        return@collect
                    }
                    val firstLoad = !initialLoadDone
                    if (firstLoad) initialLoadDone = true
                    _uiState.update { state ->
                        state.copy(
                            note = note,
                            checklistItems = items,
                            title = if (firstLoad) note.title else state.title,
                            content = if (firstLoad) note.content else state.content,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private companion object {
        const val SAVE_DELAY_MS = 500L
    }
}
