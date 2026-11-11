# Date formatting

Feed list settings offer four explicit date formats on Android, Desktop, and iOS:

| Setting | Same-year example | Previous-year example |
| --- | --- | --- |
| Day/Month | `22/08` | `22/08/2025` |
| Day.Month | `22.08` | `22.08.2025` |
| Month/Day | `08/22` | `08/22/2025` |
| Year-Month-Day | `2026-08-22` | `2025-08-22` |

Article dates use the device timezone and append the selected 12-hour or 24-hour time.
Today's articles show only the time. Day/Month remains the default, and existing saved
choices are preserved. These formats are explicit user choices, rather than formats
derived from the system locale.

The shared article formatter applies the selection to timeline, search, and widget
dates. Settings previews also show the selected format. Sync refresh timestamps,
feed parsing, and export filenames keep their existing formats.

The iOS widget extension can't read the app's Keychain-backed settings, and
`DateFormatterImpl` builds its parsing formats eagerly, which costs more memory than
the extension can spare. The widget therefore uses `FeedDateFormatter` directly, the
display half that `DateFormatterImpl` delegates to, and reads the date-format and
time-format choices from a copy the app writes to the app-group defaults
(`WidgetDateSettingsStore`) when either format changes, and once at launch for installs
that chose their formats before the copy existed. Until the app has written that copy,
the widget uses the default formats. The widget always shows the
date; the Hide date setting applies to the app's lists only.

Unit tests cover dotted dates and preference persistence. The Android and iOS
SM-009 Maestro flows check dotted timeline dates before and after relaunch.
