# Desktop UI contract

FeedFlow desktop is a calm reading workspace: a complete subscription tree, clear article hierarchy, and a comfortable reader. Jewel supplies controls, keyboard focus, semantic colors and platform conventions. Android keeps its existing presentation and AndroidX Compose dependencies. Desktop has its own presentation; shared business logic remains independent of either toolkit.

## Type and density

`DesktopDesignTokens` is the source of chrome typography and spacing. IBM Plex Sans is bundled for offline chrome rendering: 14sp/20sp body, 12sp/18sp metadata and 18sp semibold section headings. Reader font size and line height remain user controlled; article titles retain their existing user scale, and reader title remains body plus 16sp. Resource fonts use explicit variable-weight settings. Missing glyphs use the Compose/Skia/JBR fallback path; Arabic and other uncovered scripts require native verification rather than assuming the Latin font covers them.

Spacing uses 4/8/12/16/24/32dp. Navigation rows have a 32dp minimum, settings rows 36dp, icon actions 32–36dp and the main toolbar 56dp. Treat these as minimums, not fixed heights: text scaling and long translations must be allowed to expand content. Forms use a single primary action, clear secondary/cancel actions and contextual status/error feedback.

## Color and interaction

Preserve accepted white/light, charcoal/dark and black/OLED surfaces. Jewel foreground, border, focus, disabled and selection colors are semantic; blue is the action accent. OLED always uses dark control styling even if its preference is supplied independently of the dark flag. Chrome and component styles update in the mounted composition when the theme changes; cached styles must include the concrete theme values they depend on.

Normal text targets contrast 4.5:1; large text and control/focus boundaries target 3:1 against their actual adjacent surface. Do not reduce readable text opacity to communicate selection. Active selection is distinct from inactive selection, and keyboard focus stays visible when selection changes. Native captures remain the acceptance evidence for rendered contrast and clipping.

Icon actions have accessible names and tooltips containing the action and shortcut. Pointer and keyboard expose the same actions. Escape cancels transient work without mutation and returns focus to the initiator. Use concise, localized labels and inline validation; never show implementation or migration details in the product UI. Motion is limited to useful pane transitions and respects reduced motion. No decorative gradients or cards.

## Workspace and windows

Sidebar preferred width is 248dp, clamped to 200–360dp; article list preferred width is 380dp with 300dp minimum; reader minimum is 360dp. Pane eligibility depends on actual available width and requested visibility. Persist requested widths and clamp effective widths without discarding reading context. Reader text is at most 720dp wide with 32dp horizontal margins where space permits.

Preferences starts at 960×720dp and remains resizable down to 680×520dp; its navigation is 200dp at wide size and 168dp at minimum, and detail content scrolls vertically. Secondary pages use the reusable desktop dialog-window wrapper and navigator. Native title bars, owner relationships and focus return must be checked on each target OS.

Decorated Linux windows preserve their requested initial outer size when X11
replaces estimated title-bar insets after mapping. The correction can arrive
after the native window-open event for frames. A desktop-only observer restores
size once at opening or on a resize that exactly matches the inset change; it
detaches after correction, the first unrelated resize after opening, or window close. Later
user resizing remains unrestricted. The main window restores size only while
its placement is Floating, preserving maximized/fullscreen placement. Both frame
and dialog factories use the pinned Compose `SwingWindow`/`SwingDialog`
initialization hooks so the observer exists before the first native open. The
startup splash is undecorated and needs no inset correction.

## Migration and verification

Confirmation dialogs initially focus Cancel. Tab and Shift+Tab reach the primary action; Enter activates the focused action, and Escape cancels the current dialog. The message scrolls above a pinned, wrapping footer whose controls grow with text size. The reusable dialog wrapper saves the focused control and restores it when its native window becomes active again. Native focus and Compose control focus are separate asynchronous states; tests wait for both rather than assuming window activation alone restores a launcher.

The desktop theme is Jewel styling, with no Material controls/theme wrappers or shared UI imports. Retained Material icon vectors are rendered by Jewel Icon. Reader eligibility, browser fallback, pagination and sync contracts remain binding. Native window backgrounds use the same panel color as Compose content and react to theme changes. The splash window is created after the saved preference is available, so its initial palette is selected rather than guessed from the system appearance. Settings initialization remains asynchronous; this adds no independent preference reader or storage contract.

Theme behavior tests mount once, change light/dark/OLED state, verify cached selection/text/icon/focus values and retain composition identity and keyboard focus. Resource tests verify the offline font and license. Numeric-token assertions do not certify appearance.

Capture main, preferences and a form in every theme at 100%, 150% and 200% OS scale, including the largest in-app text, long English labels and Arabic RTL. Check widths 500, 741, 1024 and 1463dp, a constrained 600dp height and maximized windows.

## Bundled font provenance

Source: [Google Fonts IBM Plex Sans](https://github.com/google/fonts/tree/0b58fb370093f9a9f4ff785d94405710b79de67c/ofl/ibmplexsans), commit `0b58fb370093f9a9f4ff785d94405710b79de67c`.

- `IBMPlexSans[wdth,wght].ttf`, stored unchanged as `fonts/IBMPlexSans.ttf`: 537,244 bytes, SHA256 `3b031aa4216174205bd8471f88a49b91f093169e9e87bd5262242bc5967fe2e3`.
- `OFL.txt`: 4,456 bytes, SHA256 `7e6b2818edbd8f6a01ae80641cc8f16a51080d08fb4e532be3a0b6f74adb07da`.

The SIL Open Font License accompanies the unmodified font. Retain both when packaging. The original reserved font name is preserved.

## Presentation ownership

Desktop support lives under desktopApp's ui and testing packages. Use its strings,
reduced-motion helpers, image loader and action bundles; consume display states
and sync-error mapping from shared presentation. AppContent calls the desktop-owned
DesktopUiEnvironment facade. Jewel controls, Markdown rendering and theme adapters
live exclusively in desktopApp. Desktop owns feed-logo rendering, reader fades, menu entries
and spacing.
Android owns its presentation and
uses AndroidX Compose dependencies with the Compose BOM independently.

## Build and validation

Plain `:desktopApp:jvmTest` runs the desktop suite. Desktop compilation, tests and packaging use JBR25; the Gradle host and shared/mobile toolchains remain Java21.

Compose's default desktop bundle includes the obsolete Material runtime. Desktop excludes that dependency in both main and test configurations; Jewel supplies the controls and retained icon vectors remain available. Do not use the deprecated `androidx.compose.desktop.DesktopTheme` or `DesktopMaterialTheme` helpers: they depend on that retired runtime.

Use the configured Compose Hot Reload MCP, or `./gradlew desktopApp:run`, for UI checks. Desktop ProGuard is disabled; Android R8 remains enabled.
