# Crash & error reporting

All errors reach Firebase through one class: `telemetry/CrashReporter.kt`.

| What | Where it shows up in the Firebase console |
|---|---|
| Fatal crashes, ANRs | Crashlytics -> Issues (automatic) |
| Handled / "minor" errors (caught exceptions) | Crashlytics -> Issues -> filter **Non-fatals** |
| Error counts over time | Analytics -> Events -> `app_error` (params `error_tag`, `error_type`) |

Every report carries custom keys `screen`, `error_tag`, `build_type`, `sdk_int`, `device`, plus breadcrumbs
(activity lifecycle, screen changes) in the Logs tab.

## Using it

```kotlin
try { ... } catch (e: Exception) { CrashReporter.record("MyArea", e, "what we were doing") }

val x = CrashReporter.attempt("MyArea") { riskyCall() }.getOrNull()   // replaces runCatching

CrashReporter.recordMessage("MyArea", "invariant broken")             // problem with no exception
CrashReporter.breadcrumb("expected state, not an error")              // context only, not a report
```

## Rules
- Never report `CancellationException` (coroutine control flow); `record` and `attempt` already skip it.
- Identical errors are de-duplicated for 30 s (Crashlytics keeps only a few non-fatals per session);
  the next report notes how many were suppressed.
- Expected states are **breadcrumbs, not reports**: first launch with no notes file, notification permission
  denied, user closing Google sign-in (status 12501), zlib->raw inflate fallback, empty date/time picker input.
- The reporter itself can never throw.
