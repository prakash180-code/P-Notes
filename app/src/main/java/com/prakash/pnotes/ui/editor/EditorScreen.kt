package com.prakash.pnotes.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.prakash.pnotes.domain.model.ChecklistItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var newItemText by remember { mutableStateOf("") }

    val leave: () -> Unit = {
        viewModel.flush()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.title.isBlank()) "Note" else uiState.title) },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleChecklist() }) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = if (uiState.checklistEnabled) "Hide checklist" else "Show checklist"
                        )
                    }
                    IconButton(onClick = { viewModel.togglePin() }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (uiState.note?.isPinned == true) "Unpin" else "Pin",
                            tint = if (uiState.note?.isPinned == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            }
                        )
                    }
                    IconButton(onClick = { viewModel.archive(); leave() }) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive")
                    }
                    IconButton(onClick = { viewModel.moveToTrash(); leave() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Trash")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Title") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    placeholder = { Text("Note content") }
                )
                if (uiState.checklistEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Checklist", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newItemText,
                            onValueChange = { newItemText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add item") },
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (newItemText.isNotBlank()) {
                                    viewModel.addChecklistItem(newItemText)
                                    newItemText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add item")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.checklistItems.forEach { item ->
                        ChecklistRow(
                            item = item,
                            onToggle = { done -> viewModel.toggleChecklistItem(item, done) },
                            onDelete = { viewModel.deleteChecklistItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistRow(
    item: ChecklistItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isDone,
            onCheckedChange = { onToggle(it) }
        )
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Delete item")
        }
    }
}
