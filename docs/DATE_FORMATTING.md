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

Unit tests cover dotted dates and preference persistence. The Android and iOS
SM-009 Maestro flows check dotted timeline dates before and after relaunch.
