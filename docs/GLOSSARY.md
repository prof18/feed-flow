# FeedFlow glossary

FeedFlow's user-facing terms live in one place: the Weblate glossary of the `feedflow` project, <https://hosted.weblate.org/projects/feedflow/glossary/>. Its files are `i18n/glossary/<language>.tbx`, one per app language. Each entry holds the English term, its definition with the words to avoid, and the translation for that language.

Use these terms in `strings.xml`, the website FAQ and store copy. English copy is US English and sentence case.

## Changing the glossary

- Add or edit a term on Weblate (FeedFlow → Glossary → English → Add term to glossary, with **Terminology** ticked so it appears in every language). Weblate writes it into every `.tbx` file and delivers it in its usual pull request.
- Edits made directly in the repo must change every `i18n/glossary/*.tbx` file the same way, because each file carries the English term and its definition. Weblate picks them up from `main`.
- Keep the definition in the form "What it means. Avoid: words not to use."

## The distinctions that matter most

- **Refresh** gets new articles from the feeds. **Sync** keeps an account's data the same across devices. **Back up now** copies data to a backup account (iCloud, Dropbox, Google Drive).
- An **article** is what a feed provides (title, summary, link). The **full article** is the article's web page, which reader mode shows. An **offline copy** is a full article kept on the device. Never use "download" for offline copies, because it reads like refreshing feeds.
- **Feeds** is the collection. "Subscriptions" is only used for OPML import and export.
