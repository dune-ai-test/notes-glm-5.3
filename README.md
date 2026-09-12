# Field Notes 📓

A warm, paper-styled note-taking app for Android, built with **Kotlin + Jetpack Compose**.
Everything is local-first: notes, tags, folders and voice memos live in an on-device Room
database — no accounts, no backend.

The UI is implemented from the design mockups in `ui-sample/` (Home gallery, Editor,
Library, Settings).

## Screens

| Screen | What works |
|---|---|
| **Home** | Time-based greeting, live note count, search entry, All/Pinned chips, Grid (staggered) / List toggle, pinned hero cards, checklist cards with tappable checkboxes + progress, audio cards with real playback waveform, quote & recipe cards, long-press → pin/delete, filters sheet (sort + tag filter) |
| **Editor** | Block-based editor: paragraphs, headings, highlight callouts, interactive checklists, images (photo picker), voice memo blocks (real recording), bold toggle, tags, note color, folder, pin, delete, share as Markdown, debounced autosave |
| **Library** | Folder cards with live counts, tags with usage counts, recent activity, All/Recent/Favorites filters, note list per folder |
| **Reminder** | Upcoming list plus a mini month calendar (dotted on busy days), reminders with date & time, one-tap add-to-calendar |
| **Settings** | Local profile (editable name), appearance/language (visual), functional haptics & reduce-motion toggles, default capture, audio bitrate, notification permission, biometric app lock, clear cache, **export all notes as ZIP (Markdown + JSON + media)**, erase everything |

## Try it

1. Open the latest **Actions** run → **Artifacts** → download `FieldNotes-debug-apk`.
2. Sideload the APK on any Android 8.0+ device ("Install unknown apps" permission).
3. The app starts empty — tap **+** to write your first note.

Pushes to `main` rebuild automatically via `.github/workflows/android.yml`.

### Pre-releases

Two ways to publish the debug APK as a GitHub **pre-release** (under Releases):

- **Manual:** Actions → **Android CI** → **Run workflow** → tick
  *"Publish the debug APK as a GitHub pre-release"* → Run.
- **Tag:** push a version tag and it publishes automatically:
  ```bash
  git tag v1.1 && git push origin v1.1
  ```

## Your data

All data lives on-device. **Android Settings → Apps → Field Notes → Clear data**
wipes everything, and in-app **Settings → trash icon → Erase** does the same without
leaving the app. The app ships with no demo content.

## Tech

- Kotlin 2.0, Jetpack Compose (BOM 2024.09), Material3 styling with a fully custom design system
- Single-activity, Navigation-Compose, MVVM (ViewModel + StateFlow), manual DI (`AppContainer`)
- Room (notes/tags/folders/memos) + DataStore (settings), kotlinx-serialization for note blocks
- MediaRecorder/MediaPlayer for voice memos, Photo Picker for images, BiometricPrompt for the lock
- Variable fonts bundled in `res/font`: Geist, Geist Mono, Inter (SIL OFL)

## Scope notes (v1)

Placeholder / intentionally simple:
- Reminder tab: upcoming list + mini calendar with add-to-calendar handoff
- **Sketch** capture and the **Scan** / **AI assistant** ideas from the mockups were cut per scope decision
- Appearance is light-only; language is English (US); "Sync" is local-only by design
- Notes with audio blocks you recorded play fully; anything without a real file shows a friendly hint

## Project layout

```
app/src/main/java/com/fieldnotes/app/
  data/          Room entities/DAOs, repositories, export, audio, images
  di/            AppContainer (manual DI)
  ui/
    theme/       Colors, type (Geist/Inter), theme
    components/  Bottom bar, note cards, chips, waveform, record sheet
    home/ editor/ library/ quick/ search/ settings/   screens + viewmodels
  navigation/    AppRoot (NavHost + pill bottom bar)
```
