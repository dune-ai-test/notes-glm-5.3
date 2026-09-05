# Field Notes 📓

A warm, paper-styled note-taking app for Android, built with **Kotlin + Jetpack Compose**.
Everything is local-first: notes, tags, folders and voice memos live in an on-device Room
database — no accounts, no backend.

The UI is implemented from the design mockups in `ui-sample/` (Home gallery, Editor,
Library, Capture & Play, Settings).

## Screens

| Screen | What works |
|---|---|
| **Home** | Time-based greeting, live note count, search entry, All/Pinned chips, Grid (staggered) / List toggle, pinned hero cards, checklist cards with tappable checkboxes + progress, audio cards with real playback waveform, quote & recipe cards, long-press → pin/delete, filters sheet (sort + tag filter) |
| **Editor** | Block-based editor: paragraphs, headings, highlight callouts, interactive checklists, images (photo picker), voice memo blocks (real recording), bold toggle, tags, note color, folder, pin, delete, share as Markdown, debounced autosave |
| **Library** | Folder cards with live counts, tags with usage counts, recent activity, All/Recent/Favorites filters, note list per folder |
| **Quick minds** | Today / Capture / Play tabs, quick-capture hub (text / voice / sketch), day-streak counter, working 25-min focus timer, voice memos list with playback, tags + "New tag" |
| **Settings** | Local profile (editable name), appearance/language (visual), functional haptics & reduce-motion toggles, default capture, audio bitrate, notification permission, biometric app lock, clear cache, **export all notes as ZIP (Markdown + JSON + media)**, erase-all-and-reseed |

## Try it

1. Create a GitHub repo and push this folder:
   ```bash
   git init
   git add .
   git commit -m "Field Notes v1.0"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
2. GitHub Actions builds automatically (`.github/workflows/android.yml`).
3. Open the run in the **Actions** tab → **Artifacts** → download `FieldNotes-debug-apk`.
4. Sideload the APK on any Android 8.0+ device ("Install unknown apps" permission).

## Demo data

On first launch the app seeds the demo library from the mockups (Kyoto photo essay,
market list, memos…). Wipe it any time via **Android Settings → Apps → Field Notes →
Clear data** (the seed runs again on next start), or in-app via
**Settings → trash icon → Erase** (wipes and re-seeds immediately).

## Tech

- Kotlin 2.0, Jetpack Compose (BOM 2024.09), Material3 styling with a fully custom design system
- Single-activity, Navigation-Compose, MVVM (ViewModel + StateFlow), manual DI (`AppContainer`)
- Room (notes/tags/folders/memos) + DataStore (settings), kotlinx-serialization for note blocks
- MediaRecorder/MediaPlayer for voice memos, Photo Picker for images, BiometricPrompt for the lock
- Variable fonts bundled in `res/font`: Geist, Geist Mono, Inter (SIL OFL)

## Scope notes (v1)

Placeholder / intentionally simple:
- **Sketch** cards and the sketch capture button are visual placeholders (the seeded sketch card opens a "coming soon" screen)
- **Scan** and the **AI Ink Assistant** from the mockups were cut per scope decision
- Appearance is light-only; language is English (US); "Sync" is local-only by design
- Voice-memo *playback of the seeded demo memos* shows a friendly hint (they have no audio file); memos you record yourself play fully

## Project layout

```
app/src/main/java/com/fieldnotes/app/
  data/          Room entities/DAOs, repositories, seed, export, audio, images
  di/            AppContainer (manual DI)
  ui/
    theme/       Colors, type (Geist/Inter), theme
    components/  Bottom bar, note cards, chips, waveform, record sheet
    home/ editor/ library/ quick/ search/ settings/   screens + viewmodels
  navigation/    AppRoot (NavHost + pill bottom bar)
```
