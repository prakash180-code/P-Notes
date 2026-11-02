package com.prakash.pnotes.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prakash.pnotes.R
import com.prakash.pnotes.auth.GoogleCredentialManager
import com.prakash.pnotes.auth.GoogleIdTokenResult
import com.prakash.pnotes.domain.model.AuthState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val widgetMessage by viewModel.widgetMessage.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val authMessage by viewModel.authMessage.collectAsState()
    val signedIn = authState is AuthState.SignedIn

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { GoogleCredentialManager(context) }
    val webClientId = stringResource(R.string.default_web_client_id)
    var showLinkDialog by remember { mutableStateOf(false) }

    val launchGoogleSignIn: () -> Unit = {
        viewModel.onGoogleSignInStarted()
        scope.launch {
            when (val result = credentialManager.requestIdToken(webClientId)) {
                is GoogleIdTokenResult.Success -> viewModel.onGoogleIdToken(result.idToken)
                GoogleIdTokenResult.Cancelled -> viewModel.onGoogleSignInCancelled()
                is GoogleIdTokenResult.Failure -> viewModel.onGoogleSignInError(result.message)
            }
        }
    }

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
                .verticalScroll(rememberScrollState())
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
                    Text("Home-screen widget", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Show your notes on the home screen and open one with a tap.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = viewModel::requestWidgetPin) {
                        Text("Pin widget")
                    }
                    widgetMessage?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall)
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
                            if (state.isAnonymous) {
                                Text("Signed in anonymously")
                                Text(
                                    "Link a Google account to keep your notes with your account. " +
                                        "If this Google account is already used in P-Notes, you'll be " +
                                        "signed in to that existing account instead. Your notes on this " +
                                        "device are never deleted.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { showLinkDialog = true },
                                    enabled = viewModel.isFirebaseAvailable && !authLoading
                                ) {
                                    Text("Link Google account")
                                }
                            } else {
                                Text("Signed in with Google")
                                state.email?.let { email ->
                                    Text(email, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    "User ID: ${state.userId}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedButton(
                                onClick = viewModel::signOut,
                                enabled = !authLoading
                            ) {
                                Text("Sign out")
                            }
                        }
                        is AuthState.SignedOut -> {
                            Text(
                                if (viewModel.isFirebaseAvailable) {
                                    "Sign in with Google to keep your notes with your account."
                                } else {
                                    "Sign-in is unavailable until Firebase is configured."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = launchGoogleSignIn,
                                enabled = viewModel.isFirebaseAvailable && !authLoading
                            ) {
                                Text("Sign in with Google")
                            }
                            TextButton(
                                onClick = viewModel::signIn,
                                enabled = viewModel.isFirebaseAvailable && !authLoading
                            ) {
                                Text("Continue anonymously")
                            }
                        }
                    }
                    if (authLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Signing in...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    authMessage?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Link Google account?") },
            text = {
                Text(
                    "Your notes on this device will not be deleted. If this Google account " +
                        "is already used in P-Notes, you'll be signed in to that existing account " +
                        "instead of linking. Either way, your local notes remain available."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLinkDialog = false
                        launchGoogleSignIn()
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
