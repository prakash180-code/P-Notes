# P-Notes

Personal notes application combining Google Keep, desktop sticky notes, floating notes, and personal cloud storage concepts.

## Features
- Create and edit notes with automatic saving (debounced, flushed on exit)
- Pin/unpin notes
- Archive/unarchive notes
- Trash and restore notes
- Dedicated search screen with real-time filtering
- Note colors (stored in the data model)
- Light and dark themes (Material 3, dynamic color on Android 12+)
- Offline-first with Room database
- JSON export/backup with share sheet, plus JSON restore from the same screen
- Home-screen widget with live refresh and tap-to-open notes
- Optional Firebase cloud sync (notes + checklists, manual or every 6 hours; requires `google-services.json`)
- Google Sign-In via Android Credential Manager, with anonymous→Google account linking and conflict handling
- MVVM architecture

## Tech Stack
- Kotlin
- Jetpack Compose
- Material 3
- Room Database
- Coroutines + StateFlow
- Navigation Compose
- WorkManager
- Firebase Auth + Firestore (optional)
- MVVM

## Build
From project root:

```bash
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

## Development Status
See DEVELOPMENT_STATUS.md for current progress.
