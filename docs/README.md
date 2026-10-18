# Project documentation

Architecture, implementation, and testing guides for FeedFlow:

- [Cloud sync architecture](CLOUD_SYNC_ARCHITECTURE.md): components, storage, refresh/upload flows, merge rules, provider behavior, and limits.
- [Cloud sync regression harness](CLOUD_SYNC_TESTING.md): deterministic scenarios, platform bindings, and the `allTests` gate.
- [Live cloud sync testing](CLOUD_SYNC_LIVE_TESTING.md): real-provider setup, device scenarios, troubleshooting, and adding a provider.
- [Testing guide](TESTING.md): test structure, dependency injection, fakes, generators, and Flow testing.
- [Date formatting](DATE_FORMATTING.md): explicit article date formats, settings persistence, and display rules.
- [Desktop UI contract](DESKTOP_UI.md): Jewel typography, density, interaction, windows and native validation requirements.
- [Timeline pagination](PAGINATION.md): keyset pagination and its interaction with changing read status.
- [Podcast audio](PODCAST_AUDIO.md): enclosure detection, local storage, reader banners, platform handoff, and verification limits.
- [Reader text to speech](READER_TTS.md): extraction, playback ownership, lifecycle limits, and validation.
- [Adaptive iOS layout](IOS_ADAPTIVE_LAYOUT.md): Duo folding, window navigation, native toolbars, and validation limits.
- [Cloudflare recovery](CLOUDFLARE_FEED_RECOVERY.md): verification flow, saved website sessions, reader integration, and regression checks.
- [Sync error codes](error-codes-sync.md): synchronization error-code reference.

Agent workflows remain in [`.ai/skills`](../.ai/skills). Build and contribution instructions are in [AGENTS.md](../AGENTS.md).
