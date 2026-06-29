---
name: sentry-note-crash-fix
description: Investigate and fix FeedFlow crashes from Sentry triage notes. Use when the user provides a local note path, note URL, Gmail-exported note, Sentry issue URL, Sentry issue ID, or event ID and asks Codex to check why a crash is happening, explain the root cause, propose or implement a fix, validate it, commit it, or close/resolve the Sentry issue.
---

# Sentry Note Crash Fix

## Workflow

1. Read the user-provided note or Sentry URL first.
   - Extract `sentry_issue_id`, issue short ID, Sentry URL, event ID, release, platform, app version, exception type/message, and any stack frames.
   - If the note is a local file outside the repo, read it directly if permitted.
   - Treat the note as a lead, not ground truth. Confirm with Sentry whenever possible.

2. Fetch live Sentry context.
   - Prefer the Sentry MCP tools when available. If the namespace is not visible, use `tool_search` for `sentry issue details update issue`.
   - Fetch the issue with `get_sentry_resource` or `get_issue_details`.
   - Fetch the specific event when the note includes an event ID.
   - Use `search_issue_events`/`search_events` only when you need recurrence, release, environment, or tag distribution.
   - If MCP is unavailable, use the installed Sentry skill/CLI helper if present. If CLI auth is missing, ask the user to set `SENTRY_AUTH_TOKEN`; never ask them to paste the token into chat.

3. Explain the crash in concrete terms.
   - Identify the first relevant first-party frame and the external library/framework frame that throws.
   - Separate the immediate exception from the app behavior that triggered it.
   - Include production facts: first/last seen, occurrence count, release, environment, platform/runtime.
   - Avoid dumping raw stack traces; quote only the important frames or summarize.

4. Decide whether the issue is worth fixing.
   - Use the live Sentry context when available to judge impact: affected users, occurrence count, recurrence, releases, platforms, environments, and whether it is still happening.
   - It is acceptable to skip implementation when the issue is too exotic, affects only one or a very small number of users, depends on a very unusual local setup, or is a strange edge case with no clear product impact.
   - Also skip when the app is already handling the situation correctly and the report is just noise from expected user/environment failure, unavailable local resources, cancellation, or telemetry classification.
   - When skipping, do not invent a fix or do speculative hardening. Explain the skip reason, mention any uncertainty such as missing Sentry auth or stale counts, and stop without code changes unless the user explicitly asks to address the noise.
   - If the impact is unclear but plausibly significant, keep investigating before deciding.

5. Inspect the repo before deciding on a fix.
   - Search with `rg` for the first-party symbols, imported library package names, dependency aliases, and failing API calls.
   - Check current dependency versions in `gradle/libs.versions.toml` and module `build.gradle.kts` files.
   - If the suspected fix depends on current external package versions or migration docs, verify with authoritative sources: official docs, Maven metadata, GitHub releases, or vendor docs. Use web/curl as needed.

6. Implement the root-cause fix.
   - Prefer a narrow change that removes or guards the crashing path.
   - Follow existing project patterns and AGENTS.md rules.
   - If the fix is a dependency migration, import only the required new module and remove unused old dependencies.
   - If the fix changes business logic, add or update focused tests.
   - Use `apply_patch` for manual edits.

7. Validate.
   - Build only the affected platform first for fast feedback.
   - For desktop changes, run `./gradlew --quiet --console=plain :desktopApp:compileKotlinJvm`.
   - For shared/Kotlin/Android/Desktop changes, run `./gradlew --quiet --console=plain detekt allTests` before handoff.
   - If translation resources changed, run `.scripts/refresh-translations.sh` before Gradle checks.
   - If iOS code changed, follow the repo iOS build/format instructions.
   - If a UI was affected and the user wants to test, launch the app and leave the run session alive; otherwise stop long-running app sessions before final handoff.

8. Report outcome.
   - State why it was failing, what changed, and which validation passed.
   - If skipped, state that no code was changed and give the concrete reason it was not worth addressing.
   - Reference changed files with clickable absolute links.
   - Mention any remaining uncertainty, such as inability to reproduce locally or missing Sentry auth.

## Committing and Resolving Sentry

Only commit or resolve Sentry when the user explicitly asks.

When committing:
- Confirm the working tree scope with `git status --short` and `git diff --stat`.
- Stage only the files related to the fix.
- Use a simple one-line commit message.
- Do not include Sentry ticket numbers or `Fixes <SentryShortID>` in the commit message unless the user explicitly asks for that exact wording.
- Keep the Sentry issue reference in the Sentry resolve note instead.
- Do not push unless the user asks.

When resolving Sentry:
- Prefer `update_issue(status="resolved")` through Sentry MCP.
- Add a concise reason that includes the commit hash and the root-cause fix.
- Verify the tool response says the issue is resolved.

## Useful Sentry MCP Calls

Use these patterns when the matching tools are available:

```text
get_sentry_resource(url="<sentry issue url>")
get_sentry_resource(resourceType="event", organizationSlug="<org>", resourceId="<event id>")
search_issue_events(issueUrl="<sentry issue url>", query="environment:production", statsPeriod="30d")
update_issue(issueUrl="<sentry issue url>", status="resolved", reason="<commit and fix summary>")
```

If only a numeric issue URL is provided, pass the entire URL to Sentry tools rather than trying to infer the short ID.
