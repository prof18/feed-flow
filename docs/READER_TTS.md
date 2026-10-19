# Reader speech playback

Reader speech turns the displayed article title and body into a temporary audio file on Android and iOS. When present, the article title is spoken first, followed by the article body. The player's fallback label for an absent or blank title is not spoken. Missing and blank titles retain the renderer's distinct duplicate-heading rules and have distinct speech identities. Feed-content and URL-less reader items use the same flow. Desktop has no speech control, and there is no voice picker.

Klead reads language metadata from the original page before extraction removes its surrounding markup. An unambiguous article or main declaration takes priority, followed by the HTML/body declaration, a single content wrapper, Content-Language metadata, and Open Graph locale. FeedFlow preserves Klead's language in its cached reader wrapper and reads that stored value without reparsing the article HTML or duplicating Klead's detection rules. Feed-provided HTML also uses Klead's language metadata while retaining its original markup. Cached content without stored language metadata uses native language detection. Invalid or conflicting declarations do not select an arbitrary voice.

When metadata is absent, both platforms attempt to identify the language from a bounded sample of the narration and require at least 80% confidence. Android uses the system text classifier on Android 10 and newer; iOS uses NaturalLanguage. Older Android versions, unavailable classifiers, or inconclusive detection use the system default voice. A known language without an available matching voice produces a retryable generation error instead of reading it in a different language. One voice is used for the entire article, so individual foreign-language quotations do not switch voices.

Speech is synthesized silently during initial preparation. Once the file is ready, the app plays it through the existing podcast player. This gives speech a real duration and supports seek, pause/resume, playback speed, background playback, and the platform's system media controls. Only one audio source is active at a time. Starting a podcast or another speech item replaces the current source. Speech progress is keyed by article identity and a versioned hash of its title, content, and declared language. Changing the language regenerates narration; audio and saved positions from the previous default-voice implementation are not reused. The generated file is a single cached temporary file; closing playback or replacing the cached narration removes its file and temporary directory. The first preparation in a new app process also removes orphaned speech directories left by a forced termination.

Generation uses the captured title and displayed body text, not a fresh page fetch. Shared extraction decodes body entities once, preserves inline text and narrative content inside figures, and skips metadata, comments, captions, hidden content, scripts, styles, and media or embedded-object subtrees. The title is treated as plain text. Matching duplicate `h1` and `h2` subtrees are suppressed using the reader renderer's rule; repeated paragraphs are preserved and cached content is unchanged. Text is split at no more than 500 UTF-16 code units per native request, preferring sentence and whitespace boundaries and backing off when a boundary would split a surrogate pair. This preserves code points, not full grapheme clusters.

Static inline `display:none` hides the whole subtree. Inline `visibility:hidden` and `visibility:collapse` hide inherited text while allowing explicitly visible descendants. Declaration order and `!important` precedence are respected for recognized literal values. Extraction does not resolve stylesheets, CSS variables, or dynamic styles.

Android uses `TextToSpeech.synthesizeToFile` and iOS uses the native synthesizer to write audio before playback. Both enforce a 256 MiB output limit and a five-minute overall generation timeout. Android also applies a 15-second native initialization timeout. Generation errors leave a retryable failure state. Cancellation, replacement, and close clean up incomplete or owned temporary output. After preparation, playback follows the podcast player's normal lifecycle, including its background and system-control behavior.

Android uses the installed system TTS engine to generate each bounded text segment, then combines the resulting PCM WAV files into one Media3 item. Direct [`TextToSpeech.speak()`](https://developer.android.com/reference/android/speech/tts/TextToSpeech) uses the system speech queue, which exposes no seek or pause/resume API. The generated file lets the shared player provide measured duration, seeking, saved progress and the same media controls as podcasts.

On Android, audio-service teardown cancels pending speech preparation and releases its cached audio after saving progress. A late synthesis result cannot reopen playback after the service has stopped. Deterministic player tests cover this teardown race; native synthesis timing does not provide a reliable preparation window for a Maestro task-dismissal test.

If Android removes a completed speech cache file, Retry regenerates the captured narration and restores its saved speech position. A playback failure with the file still present retries the existing file. Cache eviction is covered by deterministic player tests.

## Validation

Focused deterministic tests cover text extraction, native generation, and shared player behavior:

Extractor tests cover missing/blank title semantics and static inline visibility. Language tests cover metadata preservation, feed fragments, voice selection, detection fallback, missing voices, and cache replacement when the language changes. Android and iOS player tests verify that display fallbacks never enter narration and that language-aware identities remain stable.

```sh
./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.tts.ReaderSpeechTextTest"
./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.tts.TtsTextExtractorTest"
./gradlew --quiet --console=plain :androidApp:testGooglePlayDebugUnitTest --tests "com.prof18.feedflow.android.audio.AndroidSpeechAudioGeneratorTest"
./gradlew --quiet --console=plain :androidApp:testGooglePlayDebugUnitTest --tests "com.prof18.feedflow.android.audio.AndroidAudioPlayerTest"
xcodebuild -project iosApp/FeedFlow.xcodeproj -scheme AudioPlaybackTests -destination 'platform=iOS Simulator,name=iPhone 18 Pro' test -quiet
```

The Android generator tests use an injected fake synthesis engine; they do not produce audible speech. Audio-player and iOS audio-playback tests cover duration, seeking, pause/resume, speed, identity, and lifecycle behavior. These tests do not prove that a device emitted sound. Validate audible title/body order, background playback and system controls on a physical Android and iOS device. Also check preparation latency with representative long articles; it depends on the installed system voice and device. Maestro can verify visible controls and player state, but cannot confirm audible output.
