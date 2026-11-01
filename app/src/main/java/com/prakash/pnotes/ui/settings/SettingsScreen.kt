package com.prakash.pnotes.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prakash.pnotes.domain.model.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val signedIn = authState is AuthState.SignedIn

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cloud Sync", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable background sync")
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = viewModel::toggleSync,
                            enabled = viewModel.isFirebaseAvailable && signedIn
                        )
                    }
                    Text(
                        when {
                            !viewModel.isFirebaseAvailable ->
                                "Firebase is not configured. Add google-services.json and enable the google-services plugin to use cloud sync."
                            !signedIn ->
                                "Sign in below to enable sync."
                            else ->
                                "Notes and checklists are backed up every 6 hours while online."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (viewModel.isFirebaseAvailable && signedIn) {
                        Button(
                            onClick = viewModel::syncNow,
                            enabled = !syncing
                        ) {
                            Text(if (syncing) "Syncing..." else "Sync now")
                        }
                        syncMessage?.let { message ->
                            Text(message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleMedium)
                    when (val state = authState) {
                        is AuthState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        is AuthState.SignedIn -> {
                            Text("Signed in")
                            Text(
                                "User ID: ${state.userId}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(onClick = viewModel::signOut) {
                                Text("Sign out")
                            }
                        }
                        is AuthState.SignedOut -> {
                            Text(
                                if (viewModel.isFirebaseAvailable) {
                                    "Sign in anonymously to sync across devices."
                                } else {
                                    "Sign-in is unavailable until Firebase is configured."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = viewModel::signIn,
                                enabled = viewModel.isFirebaseAvailable
                            ) {
                                Text("Sign in anonymously")
                            }
                        }
                    }
                }
            }
        }
    }
}
