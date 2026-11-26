# Architecture

Single-module Android app, package root `com.harsha.myapplication`, organised
**by feature** for UI and **by layer** for everything underneath.

## Layers

```
 ui/<feature>  ──▶  ui/components, ui/theme
      │
      ▼
 data/  ·  scripture/  ·  notifications/      (no dependency on ui/)
```

| Package | Responsibility |
|---------|----------------|
| `data` | Repositories (`NotesRepository`, `FolderRepository`, `PeopleRepository`, `SetupRepository`), models (`NoteItem`, `PersonRecord`, `VisitLog`…), note image storage |
| `scripture` | Pure-ish domain logic: `ScriptureDetector`, `BookMatcher`, `PhoneticFold`, `BibleData`, `JwpubImporter`, `ScriptureReference` |
| `notifications` | `AppNotifications` – channel creation and permission state |
| `ui.theme` | Colour, typography, shapes, motion tokens, `AppearanceRepository` |
| `ui.components` | Reusable composables used by 2+ features |
| `ui.editor.richtext` | Editor engine, no UI: spans (`FormatSpan`, `FmtType`), list markers, per-line alignment, span/JSON codec, styled-text builder, inline-image layout |
| `ui.editor.components` | Reusable editor UI: `SelectionToolbar`, `FormatPanel`, `AlignPanel`, `FindBar`, `AttachedImage`/`ImageViewer`, panel building blocks, tooltips |
| `ui.<feature>` | Screens and feature-private composables |

## Rules

1. `data`, `scripture` and `notifications` never import from `ui`.
2. A feature package does not import another feature's screens. Cross-feature
   needs go through `MainActivity` navigation or are promoted to `ui.components` / `data`.
3. Top-level helpers used by one feature stay `private`/`internal` in that feature.
4. Assets live in a named subfolder (`assets/scripture/…`).

## Known follow-ups

- `ui/notes/NoteEditorScreen.kt` still holds one ~2 100-line `NoteEditorView` function. Its
  local functions (`toggleFmt`, `applyList`, `undo`, `insertScripture`, ...) close over ~30 state
  variables. Move them into a `RichTextState` holder in `ui/editor/richtext` so any screen can
  drive `SelectionToolbar` / `FormatPanel` / `AlignPanel` without copying logic.
- `MainActivity.kt` holds navigation, shared colour aliases (`Purple`, `CardBg`, …) and
  sample data (`DailyItem`, `sampleDaily`); move these to `navigation/` and `ui/theme`.
- `HomeScreen` currently reaches into `ui.people` (`PersonCard`, detail/edit screens);
  extract shared person UI into `ui/components` to satisfy rule 2.
- Move to state holders / `ViewModel`s with a DI container as the app grows.
- Replace the placeholder `app_name` ("My Application") and package id
  `com.harsha.myapplication` before a Play Store release (changing `applicationId`
  creates a new app identity, so do it deliberately).
