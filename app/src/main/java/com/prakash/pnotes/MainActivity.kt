package com.prakash.pnotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prakash.pnotes.data.export.ExportRepository
import com.prakash.pnotes.di.AppModule
import com.prakash.pnotes.ui.archive.ArchiveScreen
import com.prakash.pnotes.ui.archive.ArchiveViewModel
import com.prakash.pnotes.ui.editor.EditorScreen
import com.prakash.pnotes.ui.editor.EditorViewModel
import com.prakash.pnotes.ui.export.ExportScreen
import com.prakash.pnotes.ui.home.HomeScreen
import com.prakash.pnotes.ui.home.HomeViewModel
import com.prakash.pnotes.ui.search.SearchScreen
import com.prakash.pnotes.ui.search.SearchViewModel
import com.prakash.pnotes.ui.settings.SettingsScreen
import com.prakash.pnotes.ui.settings.SettingsViewModel
import com.prakash.pnotes.ui.theme.PNotesTheme
import com.prakash.pnotes.ui.trash.TrashScreen
import com.prakash.pnotes.ui.trash.TrashViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val repository by lazy { AppModule.getRepository(this) }

    /** Note id passed by a widget tap, consumed once by the nav host. */
    private val pendingNoteId = MutableStateFlow<String?>(null)

    private val homeViewModel: HomeViewModel by viewModels {
        viewModelFactory { initializer { HomeViewModel(application, repository) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            pendingNoteId.value = intent?.getStringExtra(EXTRA_NOTE_ID)
        }
        setContent {
            PNotesTheme {
                PNotesNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNoteId.value = intent.getStringExtra(EXTRA_NOTE_ID)
    }

    @Composable
    fun PNotesNavHost() {
        val navController = rememberNavController()
        val pendingNote by pendingNoteId.collectAsState()

        LaunchedEffect(pendingNote) {
            val noteId = pendingNote ?: return@LaunchedEffect
            navController.navigate("editor/$noteId") {
                launchSingleTop = true
            }
            pendingNoteId.value = null
        }

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNoteClick = { id -> navController.navigate("editor/$id") },
                    onAddClick = { navController.navigate("editor/new") },
                    onArchiveClick = { navController.navigate("archive") },
                    onTrashClick = { navController.navigate("trash") },
                    onSearchClick = { navController.navigate("search") },
                    onExportClick = { navController.navigate("export") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            composable(
                "editor/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { entry ->
                val noteId = entry.arguments?.getString("noteId")
                val viewModel: EditorViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = viewModelFactory {
                        initializer { EditorViewModel(application, repository, noteId) }
                    }
                )
                EditorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable("archive") { entry ->
                val viewModel: ArchiveViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = viewModelFactory {
                        initializer { ArchiveViewModel(application, repository) }
                    }
                )
                ArchiveScreen(
                    viewModel = viewModel,
                    onNoteClick = { id -> navController.navigate("editor/$id") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("trash") { entry ->
                val viewModel: TrashViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = viewModelFactory {
                        initializer { TrashViewModel(application, repository) }
                    }
                )
                TrashScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable("settings") { entry ->
                val viewModel: SettingsViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = viewModelFactory {
                        initializer { SettingsViewModel(application, repository) }
                    }
                )
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable("search") { entry ->
                val viewModel: SearchViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = viewModelFactory {
                        initializer { SearchViewModel(application, repository) }
                    }
                )
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNoteClick = { id -> navController.navigate("editor/$id") }
                )
            }

            composable("export") {
                val exportRepository = remember {
                    ExportRepository(applicationContext, repository)
                }
                ExportScreen(
                    exportRepository = exportRepository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    companion object {
        /** Intent extra used by the home-screen widget to open a specific note. */
        const val EXTRA_NOTE_ID = "noteId"
    }
}
