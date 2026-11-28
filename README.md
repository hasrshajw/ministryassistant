# Ministry Assistant

An offline-first Android app (Kotlin + Jetpack Compose, Material 3 Expressive) for organising
personal Bible study and ministry work: notes with automatic scripture detection, a people
directory with visit logs, a daily text screen, monthly hour goals, and importable Bible
publications.

## Features

| Area | What it does |
|------|--------------|
| **Notes** | Rich note editor, folders, categories, image attachments, search & sort |
| **Scripture detection** | Recognises references while typing — English abbreviations, Telugu names, romanised/misspelled spellings (fuzzy match with confidence) and names from imported Bibles |
| **Bible** | Book browser / reader, `.jwpub` importer |
| **People** | Directory of return visits, studies and inactive contacts; visit logs; edit & detail screens |
| **Home** | Daily text, recommendations, hours entry, upcoming visits |
| **Setup & settings** | First-run wizard (notifications, monthly goal), appearance/theme customisation, profile |

## Tech stack

- Kotlin 2.4, Jetpack Compose (BOM 2026.11.01), Material 3 1.5.0 stable (Expressive)
- Android Gradle Plugin 9.4, Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`)
- `minSdk 24` · `compileSdk 37.1` / `targetSdk 37` · Java 11 source level
- Local persistence only (SharedPreferences + app files) — no network permissions required

## Getting started

```bash
git clone https://github.com/hasrshajw/ministryassistant.git
cd ministryassistant
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # unit tests
./gradlew installDebug           # install on a connected device/emulator
```

Open the folder in a recent Android Studio release and run the `app` configuration.

## Project layout

```
app/src/main/
├── java/com/harsha/myapplication/
│   ├── MainActivity.kt        # entry point, navigation scaffold, bottom bar
│   ├── data/                  # repositories & models (notes, folders, people, setup)
│   ├── scripture/             # scripture detection, book matching, Bible data, JWPUB import
│   ├── notifications/         # notification channel helper
│   └── ui/
│       ├── theme/             # colours, type, shapes, motion, appearance settings
│       ├── components/        # shared composables (top bar, search/sort, transitions, back)
│       ├── home/              # home, daily text, add-hours sheet
│       ├── notes/             # notes list, note editor screen, folder pickers
│       ├── editor/            # reusable rich-text editor kit
│       │   ├── richtext/      #   spans, lists, alignment, codec, styled text (no UI)
│       │   └── components/    #   selection toolbar, format/align panels, find bar, images
│       ├── people/            # directory, person detail & edit
│       ├── bible/             # book browser, Bible import, scripture bottom sheet
│       ├── settings/          # settings, appearance, customise, Gen-Z mode
│       ├── profile/  setup/  about/  legal/
├── assets/scripture/          # Telugu book-name variants used by the matcher
└── res/                       # fonts, icons, strings, themes, XML rules
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for dependency rules and conventions.

## Contributing

- Follow `.editorconfig` (4-space Kotlin, 120 cols).
- Keep one feature per package under `ui/`; shared UI goes in `ui.components`.
- UI packages may depend on `data`, `scripture`, `ui.theme` and `ui.components` — never on each other's internals.
- CI: add `.github/workflows/android-ci.yml` (tests, lint, debug build) when ready.

## License

No license has been chosen yet. Until one is added, all rights are reserved by the author.
