# Field Notes 📓

A warm, paper-styled note-taking app for Android, built with **Kotlin + Jetpack Compose**.
Everything is local-first: notes, folders, voice memos and reminders live in an on-device Room
database — no accounts, no backend.

The UI is implemented from the design mockups in `ui-sample/` (Home gallery, Editor,
Library, Settings).

## Features

### Notes & editor
- **Block-based editor** — paragraphs, headings, highlight callouts, interactive checklists, images (photo picker), voice-memo blocks with live waveform recording
- **Editor toolbar** — Bold (with active/disabled states), Text, Checklist, Image, add-block sheet, voice recorder; symmetric floating pill with gesture-inset spacing
- **Tap-to-write** — the editor body fills the available height (grows under the keyboard); tapping below the blocks appends and focuses a paragraph
- **Debounced autosave with dirty tracking** — opening/closing without edits never touches the note
- **Lazy drafts** — new notes exist only in memory until you type; emptied drafts clean themselves up
- **Per-note lock** — biometric-gate individual notes; locked cards hide content everywhere and demand authentication to open
- Note options: pin, folder assignment (body card *and* ⋯ sheet), 6-color card palette with named swatches, share, delete to trash
- **Share as Markdown** — from the editor and from Home's long-press menu

### Home
- Time-based greeting, live filtered note count, date line
- Search bar → dedicated search over titles and bodies with **match highlighting**
- Filter chips (All with count / Pinned) and **Sort sheet** (Recent / Oldest / A–Z / Custom) on one row, with Grid-List toggle and Reorder on the right
- **Reorder mode** — drag-to-rearrange cards (auto-switches to List view), persisted as a custom order
- Grid/List toggle — staggered pastel cards or compact rows with folder-tinted tiles, folder-name meta and two-line previews
- Six card styles: pinned hero, checklist (tappable checkboxes + progress), audio (play + waveform), quote, text, sketch — every card shows its folder
- Long-press menu: Pin / Share as Markdown / Change colour / Delete (trash confirmation + **undo snackbar**)
- Filter-aware empty states with correct guidance

### Library
- **Folder grid** — pastel cards with per-folder icon, live note counts and "updated" recency
- **Folder creation** — name, 20 icon choices, 6-colour palette (matching the note palette, white included)
- Per-folder note lists (trash-aware)

### Zone (tools hub)
- **Mileage** — hub card → history screen: all-time summary (average km/L, total distance, litres, amount spent), per-vehicle fill-up history with date / odometer range / litres / cost / km-per-litre badge, and a "Log fill-up" dialog that computes litres from price and amount paid
- **Passwords** — hub card → biometric-gated vault screen: add, reveal and remove entries (in-memory demo; persistence planned)

### Reminders
- Upcoming list + **mini month calendar** (dotted on busy days), swipeable between the two
- Date/time picker with **Once / Daily / Weekly** repeat
- **Real notifications** — exact alarms (inexact fallback), Complete and Snooze-1h actions on the notification, tap opens the linked note, alarms re-registered after reboot
- Swipe right to complete, snooze 1 hour or tomorrow 9:00, completed section with restore

### Trash
- Everything deletes softly: restore, delete-forever (with media cleanup), empty-trash
- Items auto-purge after **30 days**; locked notes stay protected in trash

### Search
- Matches titles and bodies (locked note bodies excluded), result count, empty-query and no-match states

### Settings
- **Dark / Light / System** appearance — window background and system-bar icons follow the app theme
- Profile name, haptics, audio quality (High/Standard bitrate), notification permission
- **Biometric app lock** with **auto-lock timeout** (Immediately / 1 / 5 / 15 minutes in background)
- **Export all notes** — ZIP with structured JSON, Markdown and all media (note audio + memo recordings)
- **Import backup** — restores structured v1.1.7+ backups *and* legacy pre-JSON exports
- **Scheduled weekly auto-backup** (WorkManager, keeps the newest 4, device-only) plus Back-up-now
- Clear cache, Trash, Erase-everything (with re-seeded starter folders)
- Real version from BuildConfig, What's-new, Help, Rate-the-app

### Platform
- Local-first Room database (notes, folders, memos, reminders) + DataStore settings — no accounts, no cloud
- Swipe navigation between **Home · Zone · Reminders · Library · Settings** (icon-only bottom bar)
- WCAG-AA label contrast in both themes, Geist / Geist Mono / Inter variable typography
- Trash, backup and search integrate with per-note locks and folders

## Try it

1. Open the latest **Actions** run → **Artifacts** → download `FieldNotes-debug-apk`, or grab a
   pre-release from the **Releases** page.
2. Sideload the APK on any Android 8.0+ device ("Install unknown apps" permission).
3. The app starts with starter folders and no notes — tap **+** to write your first one.

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
wipes everything, and in-app **Settings → Privacy & Data → Erase all data** does the same without
leaving the app (starter folders are restored). The app ships with no demo content.

## Tech

- Kotlin 2.0, Jetpack Compose (BOM 2024.09), Material3 styling with a fully custom design system
- Single-activity, Navigation-Compose + HorizontalPager, MVVM (ViewModel + StateFlow), manual DI (`AppContainer`)
- Room (notes/folders/memos/reminders) + DataStore (settings), kotlinx-serialization for note blocks and backups
- MediaRecorder/MediaPlayer for voice memos, Photo Picker for images, BiometricPrompt for locks, AlarmManager + WorkManager for reminders and scheduled backups
- Variable fonts bundled in `res/font`: Geist, Geist Mono, Inter (SIL OFL)

## Scope notes (v1.x)

Placeholder / intentionally simple:
- **Zone → Passwords** is an in-memory demo (entries clear on exit); encrypted persistence is planned
- **Sketch** capture and the **Scan** / **AI assistant** ideas from the mockups were cut per scope decision
- Language is English (US); "Sync" is local-only by design; appearance follows the app's two themes
- Reminder notifications don't surface in-app badges; backups are device-only unless you move the ZIP yourself

## Project layout

```
app/src/main/java/com/fieldnotes/app/
  data/          Room entities/DAOs, repositories, export/import, backups, reminders, audio, images
  di/            AppContainer (manual DI)
  ui/
    theme/       Colors, type (Geist/Inter), theme
    components/  Bottom bar, note cards, chips, waveform, record sheet, biometric prompt
    home/ editor/ library/ quick/ zone/ search/ trash/ settings/   screens + viewmodels
  navigation/    AppRoot (pager + pill bottom bar)
```
