# FeedFlow glossary

The canonical English words for FeedFlow's user-facing concepts. Use them in `strings.xml`, the website FAQ and store copy, and translate each one the same way everywhere. The same terms are the Weblate glossary of the `feedflow` project.

English copy is US English and sentence case.

| Term | Use | Avoid | Definition for translators |
| --- | --- | --- | --- |
| feed | feed, feeds | feed source, source, subscription, channel | A website's RSS, Atom or JSON feed that the user follows. |
| Feeds | Feeds (section and screen title) | Feed Sources | The list of all feeds the user follows, grouped by category. |
| pinned feeds | Pinned feeds, Pin, Unpin | Pinned Feed Sources, favorites | Feeds and categories the user keeps at the top of the sidebar. |
| subscriptions | only for OPML import and export | subscriptions anywhere else | The feed list as stored in an OPML file, the format other feed readers use. |
| feed name | feed name | feed source | The title of a feed, shown on each article. |
| article | article, articles | item, post, entry, story | One entry of a feed: title, summary and link, which FeedFlow gets when it refreshes feeds. |
| full article | full article | article content, web content | The whole text of an article's web page, which FeedFlow gets from the website for reader mode. |
| offline copy | offline copy, offline copies | download, save, prefetch, cache | A full article stored on the device so it can be read without a connection. |
| image cache | Clear image cache | downloaded images | Images stored on the device so they load faster. Only used for images. |
| refresh | Refresh feeds, Background refresh | download, sync (for getting new articles), reload | Check the feeds for new articles. |
| sync | sync, syncing | upload, refresh | Keep feeds, read status and bookmarks the same across the user's devices through an account. |
| back up | Back up now (verb), backup (noun) | Backup (as a verb), Upload changes | Copy feeds, read status and bookmarks to a backup account (iCloud, Dropbox, Google Drive). |
| account | account, sync account | service, provider | A connected iCloud, Dropbox, Google Drive, FreshRSS, Miniflux, Feedbin or BazQux account. |
| bookmark | Bookmarks, bookmark (verb), bookmarked | saved, save for later, favorites | Articles the user keeps; they are never deleted automatically. |
| category | category, New category | + Category, folder, group | A user-made group of feeds. |
| Uncategorized | Uncategorized | No category | The group of feeds that have no category. |
| feed settings | Feed settings | Edit feed | The screen with the options of a single feed. |
| open website | Open website | Open feed website | Open the homepage of the feed's website in the browser. |
| reader mode | Reader mode | Reader Mode, reader view | FeedFlow's built-in, distraction-free view of a full article. |
| mark as read | Mark as read, Mark as unread, Mark all as read | Toggle read | Set an article's read status. |

"Background sync" settings are still labelled with sync; they move to "Background refresh" when the notification and refresh settings are merged.
