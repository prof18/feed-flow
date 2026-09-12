# Project documentation

Architecture, implementation, and testing guides for FeedFlow:

- [Cloud sync architecture](CLOUD_SYNC_ARCHITECTURE.md): components, storage, refresh/upload flows, merge rules, provider behavior, and limits.
- [Cloud sync regression harness](CLOUD_SYNC_TESTING.md): deterministic scenarios, platform bindings, and the `allTests` gate.
- [Live cloud sync testing](CLOUD_SYNC_LIVE_TESTING.md): real-provider setup, device scenarios, troubleshooting, and adding a provider.
- [Testing guide](TESTING.md): test structure, dependency injection, fakes, generators, and Flow testing.
- [Timeline pagination](PAGINATION.md): keyset pagination and its interaction with changing read status.
- [Sync error codes](error-codes-sync.md): synchronization error-code reference.

Agent workflows remain in [`.ai/skills`](../.ai/skills). Build and contribution instructions are in [AGENTS.md](../AGENTS.md).
