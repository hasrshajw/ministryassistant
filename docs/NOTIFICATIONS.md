# Notifications & reminders

Everything lives in `notifications/`. Reminders are **OS alarms**, not app code that has to be running, so they
fire with the app closed for weeks, in Doze and in battery saver.

## How a reminder reaches the user

| Step | Where | What guarantees it |
|------|-------|--------------------|
| Schedule | `ReminderScheduler.setAlarm` | `setAlarmClock` (exempt from Doze / battery saver) → `setExactAndAllowWhileIdle` → `setAndAllowWhileIdle` (used while "Alarms & reminders" is not allowed) |
| Survive reboot / update / clock change | `BootReceiver` | `BOOT_COMPLETED`, `QUICKBOOT_POWERON` (HTC, Xiaomi…), `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, exact-alarm permission granted. Boot and update also **catch up** anything missed |
| Fire | `ReminderReceiver` | Wake lock + `goAsync()`, work off the main thread. The next occurrence is **re-armed first**, then the notification is built |
| Watchdog | `heartbeat` alarm (every 6 h) | Re-registers every alarm and catches up, in case one was lost |
| Show | `NotificationHelper` | Gen-Z copy falls back to plain text if it fails; channels are created before posting |
| Exact permission (Android 12+) | `ExactAlarmPrompt` | Asks (at most weekly) for "Alarms & reminders" and upgrades saved reminders as soon as it is granted |

## Rules that keep it correct

- **Scheduling and cancelling build the same `Intent` (action + data URI)** (`personIntent` / `systemIntent`). If they differ,
  the system cannot match the `PendingIntent` and the cancel silently does nothing.
- Every alarm carries the visit time it was made for. If the person's visit has moved since, the alarm is ignored
  (`ReminderLogic.isScheduledTimeOutdated`).
- All timing decisions are pure functions in `ReminderLogic` and are unit-tested (`ReminderLogicTest`).
- A daily summary is delivered whenever its (user-chosen) time arrives; there is no fixed "before 11 AM" cut-off.

## What can still stop a reminder (outside the app's control)

- **Force stop** (Settings → App info → Force stop) cancels all of the app's alarms until the app is opened once.
- Some OEM "battery managers" (Xiaomi, Oppo, Vivo, Realme, OnePlus, Huawei) kill apps and remove their alarms when the app is
  swiped away. Users on those phones should allow auto-start / "no restrictions" battery for the app.
- Turning off "Alarms & reminders" or the notification permission / channel in system settings.
- `USE_EXACT_ALARM` (auto-granted) is restricted by Google Play to alarm-clock and calendar apps, so this app uses
  `SCHEDULE_EXACT_ALARM` plus the prompt above.
