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
- [ ] Enable the Google provider in Firebase Console → Authentication → Sign-in method
- [ ] Runtime verification of Google Sign-In on a device

### Notes
Firebase is configured: `app/google-services.json` is present (project `p-notes-30981`, package `com.prakash.pnotes`), the google-services classpath/plugin are enabled, and the debug SHA-1 is registered. Anonymous Auth must also be enabled in the console for "Continue anonymously". Cloud sync remains optional and is not enforced.

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

## Phase 5: Google Sign-In (IMPLEMENTED, pending device verification)

### Completed
- [x] Firebase configured: google-services.json present, google-services plugin enabled, debug SHA-1 registered
- [x] Credential Manager (`androidx.credentials:credentials:1.3.0`, `credentials-play-services-auth:1.3.0`) + Google ID token library (`googleid:1.1.1`) — no deprecated GoogleSignInClient
- [x] `GoogleCredentialManager` requests a Google ID token using `R.string.default_web_client_id`; handles success, user cancellation, no-credential, and provider errors
- [x] `GoogleSignInPolicy` (pure, unit tested) decides: link anonymous session, plain sign-in, or refuse when already signed in with Google
- [x] `FirebaseAuthRepository.signInWithGoogle(idToken)` uses `GoogleAuthProvider.getCredential(idToken, null)`; anonymous sessions are linked via `linkWithCredential` so the user id is preserved
- [x] Collision fallback: if the Google account already exists, sign in to it; the anonymous account is never deleted and the Room database is untouched
- [x] `AuthState.SignedIn` now carries `email` and `isAnonymous`; Settings shows email/anonymous mode with sign-out
- [x] Settings UI: Google button, "Continue anonymously", link confirmation dialog, loading/success/cancel/error messages
- [x] `assembleDebug` and `testDebugUnitTest` pass (7 unit tests: NoteModelTest 3, GoogleSignInPolicyTest 4)

### Pending
- [ ] Runtime verification of the successful sign-in path when Play services is fixed on the test device
- [ ] Anonymous Auth must stay enabled in the Firebase Console for "Continue anonymously"

### On-device findings (moto g32 - 13, GMS 26.42.31)
- Settings renders the new Account card: Google button, "Continue anonymously", cancelled-state message
- Tapping "Sign in with Google" starts Credential Manager correctly
- The Google account sheet crashes inside `com.google.android.gms.ui` (`AssistedSignInActivity`, `Bitmap.createBitmap` zero-size) before it opens — a Play services bug on this build, even after cache trim and reboot
- P-Notes recovers gracefully: loading resets, "Google sign-in cancelled." is shown, no app crash, notes and widget intact
- Full end-to-end sign-in (token exchange, link/conflict handling) is therefore build-verified and unit-verified, but not yet user-visible on this device
