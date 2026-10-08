# Reader speech playback

Reader speech turns the displayed article title and body into a temporary audio file on Android and iOS. The title is spoken first, followed by the article body. Feed-content and URL-less reader items use the same flow. Desktop has no speech control, and there is no voice picker: both platforms use the system's default voice.

Speech is synthesized silently during initial preparation. Once the file is ready, the app plays it through the existing podcast player. This gives speech a real duration and supports seek, pause/resume, playback speed, background playback, and the platform's system media controls. Only one audio source is active at a time. Starting a podcast or another speech item replaces the current source. Speech progress is keyed by article identity and a hash of its title and content, so different text does not reuse the same saved position. The generated file is a single cached temporary file; closing playback or replacing the cached narration removes its file and temporary directory. The first preparation in a new app process also removes orphaned speech directories left by a forced termination.

Generation uses the captured title and displayed body text, not a fresh page fetch. Shared extraction decodes body entities once, preserves inline text and narrative content inside figures, and skips metadata, comments, captions, hidden content, scripts, styles, and media or embedded-object subtrees. The title is treated as plain text. Matching duplicate `h1` and `h2` subtrees are suppressed using the reader renderer's rule; repeated paragraphs are preserved and cached content is unchanged. Text is split at no more than 500 UTF-16 code units per native request, preferring sentence and whitespace boundaries and backing off when a boundary would split a surrogate pair. This preserves code points, not full grapheme clusters.

Android uses `TextToSpeech.synthesizeToFile` and iOS uses the native synthesizer to write audio before playback. Both enforce a 256 MiB output limit and a five-minute overall generation timeout. Android also applies a 15-second native initialization timeout. Generation errors leave a retryable failure state. Cancellation, replacement, and close clean up incomplete or owned temporary output. After preparation, playback follows the podcast player's normal lifecycle, including its background and system-control behavior.

Android uses the installed system TTS engine to generate each bounded text segment, then combines the resulting PCM WAV files into one Media3 item. Direct [`TextToSpeech.speak()`](https://developer.android.com/reference/android/speech/tts/TextToSpeech) uses the system speech queue, which exposes no seek or pause/resume API. The generated file lets the shared player provide measured duration, seeking, saved progress and the same media controls as podcasts.

On Android, audio-service teardown cancels pending speech preparation and releases its cached audio after saving progress. A late synthesis result cannot reopen playback after the service has stopped. Deterministic player tests cover this teardown race; native synthesis timing does not provide a reliable preparation window for a Maestro task-dismissal test.

## Validation

Focused deterministic tests cover text extraction, native generation, and shared player behavior:

```sh
./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.tts.ReaderSpeechTextTest"
./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.tts.TtsTextExtractorTest"
./gradlew --quiet --console=plain :androidApp:testGooglePlayDebugUnitTest --tests "com.prof18.feedflow.android.audio.AndroidSpeechAudioGeneratorTest"
./gradlew --quiet --console=plain :androidApp:testGooglePlayDebugUnitTest --tests "com.prof18.feedflow.android.audio.AndroidAudioPlayerTest"
xcodebuild -project iosApp/FeedFlow.xcodeproj -scheme AudioPlaybackTests -destination 'platform=iOS Simulator,name=iPhone 17 Pro' test -quiet
```

The Android generator tests use an injected fake synthesis engine; they do not produce audible speech. Audio-player and iOS audio-playback tests cover duration, seeking, pause/resume, speed, identity, and lifecycle behavior. These tests do not prove that a device emitted sound. Validate audible title/body order, background playback and system controls on a physical Android and iOS device. Also check preparation latency with representative long articles; it depends on the installed system voice and device. Maestro can verify visible controls and player state, but cannot confirm audible output.
