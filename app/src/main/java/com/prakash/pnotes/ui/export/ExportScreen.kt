package com.prakash.pnotes.ui.export

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.prakash.pnotes.data.export.ExportRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    exportRepository: ExportRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        result = null
        scope.launch {
            try {
                val summary = exportRepository.importFromJson(uri)
                result = "Imported ${summary.imported} note(s), skipped ${summary.skipped}."
            } catch (e: Exception) {
                result = "Import failed: ${e.message}"
            } finally {
                importing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export / Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Backup to JSON", style = MaterialTheme.typography.titleMedium)
                    Text("Export all notes and checklists to a JSON file.")
                    Button(
                        onClick = {
                            exporting = true
                            result = null
                            scope.launch {
                                try {
                                    val path = exportRepository.exportToJson()
                                    result = "Saved to $path"
                                    shareFile(context, path)
                                } catch (e: Exception) {
                                    result = "Export failed: ${e.message}"
                                } finally {
                                    exporting = false
                                }
                            }
                        },
                        enabled = !exporting && !importing
                    ) {
                        Text(if (exporting) "Exporting..." else "Export & Share")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Restore from JSON", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Import a P-Notes backup. Existing notes are only replaced " +
                            "when the backup copy is newer."
                    )
                    Button(
                        onClick = {
                            result = null
                            importLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream"
                                )
                            )
                        },
                        enabled = !exporting && !importing
                    ) {
                        Text(if (importing) "Importing..." else "Choose backup file")
                    }
                }
            }

            result?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun shareFile(context: Context, path: String) {
    val file = File(path)
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share backup"))
}
