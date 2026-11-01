# Development Status - P-Notes

## Phase 1: Project setup and local notes (COMPLETE)

### Completed
- [x] Android project structure created
- [x] Gradle configuration (AGP 8.5.2, Kotlin 1.9.24, Compose BOM)
- [x] Room database setup (NoteEntity, ChecklistItemEntity, DAOs, Database)
- [x] Domain models (Note, ChecklistItem)
- [x] Repository layer
- [x] Dependency injection module
- [x] Material 3 theming (light/dark)
- [x] MainActivity with Navigation
- [x] Home screen with notes grid/list
- [x] Note editor with auto-save (debounced)
- [x] Pin/unpin functionality
- [x] Archive screen and functionality
- [x] Trash screen with restore/empty
- [x] Search functionality
- [x] Dedicated Search screen (ui/search) with real-time filtering
- [x] Unit test for Note model
- [x] README.md created
- [x] Checklist editor UI (add/toggle/delete checklist items in note editor)
- [x] Export/Backup to JSON (data/export, ui/export) with FileProvider share

## Phase 2: Home-screen widgets (COMPLETE)

### Completed
- [x] App widget provider registration in AndroidManifest.xml
- [x] AppWidgetProviderInfo XML metadata (note_widget_info.xml)
- [x] Widget layout (note_widget.xml) and item layout (widget_item_note.xml)
- [x] NoteAppWidgetProvider (AppWidgetProvider)
- [x] NoteWidgetRemoteViewsService + NoteWidgetRemoteViewsFactory
- [x] Build succeeds
- [x] APK installed and widget provider registered on device ZD222BXJW2
- [x] Widget refreshes when notes change (notifyAppWidgetViewDataChanged)
- [x] Empty-state view shown when there are no active notes
- [x] Widget item taps open the tapped note (mutable fill-in intent + noteId handling)

## Phase 3: Sync/Cloud (Firebase) (SCAFFOLDED)

### Completed
- [x] Firebase BOM, Auth and Firestore dependencies in app/build.gradle
- [x] Domain models: AuthState, SyncStatus, SyncResult
- [x] AuthRepository interface + FirebaseAuthRepository (auth/FirebaseAuthRepository.kt)
- [x] CloudSyncRepository interface + FirestoreSyncRepository (data/cloud/FirestoreSyncRepository.kt)
- [x] NoteSyncWorker (WorkManager periodic sync, 6-hour interval) + SyncInitializer
- [x] SettingsViewModel + SettingsScreen wired to real auth state and persisted sync toggle
- [x] Sync uploads local notes and merges newer remote notes by updatedAt
- [x] Checklist items are embedded in each note document and restored on download
- [x] Manual "Sync now" action in Settings with progress and result message
- [x] Build succeeds and unit tests pass

### Pending
- [ ] Add google-services.json from Firebase Console
- [ ] Uncomment the google-services classpath (build.gradle) and plugin (app/build.gradle)

### Notes
Firebase SDKs compile and run without google-services.json; every Firebase call is wrapped in try/catch and the Settings screen reports that sync is unavailable. To enable cloud sync:
1. Create a Firebase project at https://console.firebase.google.com
2. Register an Android app with package com.prakash.pnotes
3. Download google-services.json and place it at app/google-services.json
4. Uncomment the google-services classpath and plugin in build.gradle files
5. Sign in anonymously from Settings and enable sync

## Phase 4: Stability fixes (COMPLETE)

### Completed
- [x] Editor ViewModel scoped to the navigation entry (each note gets its own instance)
- [x] Checklist items now load (note + items combined in one flow)
- [x] Checklist mode can be toggled from the editor toolbar
- [x] Auto-save no longer overwrites in-progress typing; pending edits flush on exit
- [x] New notes are created lazily on first edit instead of on open
- [x] Export runs off the main thread, includes archived/trashed notes, and shares correctly
- [x] Search uses a single combined flow instead of leaking a collector per keystroke
- [x] Emptying trash also deletes orphaned checklist rows
- [x] Sync toggle is persisted and cancels/starts WorkManager accordingly
- [x] JSON backup can be restored from the Export screen (newer backup copy wins)
- [x] Restoring a backup also restores checklist items
- [x] Instrumented tests cover the export/import round-trip and the newer-local-wins rule
