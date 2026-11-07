---
title: "How do I sync FeedFlow between my devices?"
weight: 20
featured: true
---

Go to **Settings → Feeds & accounts → Accounts** and connect the same account on every device. FeedFlow supports two kinds of accounts:

- **Backup services**: iCloud, Dropbox and Google Drive. Your feeds, categories, read articles and bookmarks are saved to a file in your own storage and picked up by your other devices.
- **Reader services**: FreshRSS, Miniflux, BazQux and Feedbin. Your feeds and read status live on the server, and FeedFlow syncs with it.

You can connect one account at a time. Sync runs when feeds are refreshed and when you close the app; on iPhone and iPad, changes are also uploaded every couple of minutes while FeedFlow is open. **Back up now** sends your latest changes right away. Your other devices pick up the changes the next time they refresh, so on iPhone and iPad, where background refresh is up to iOS, open the app to see them.

Not every account is available in every version:

| Version | iCloud | Dropbox | Google Drive | Reader services |
|---|---|---|---|---|
| iPhone, iPad, Mac (App Store) | ✓ | ✓ | ✓ | ✓ |
| Android (Google Play) | | ✓ | ✓ | ✓ |
| Android (F-Droid) | | | | ✓ |
| Windows, Linux `.deb`, `.rpm`, AppImage | | ✓ | ✓ | ✓ |
| Linux (Flathub) | | | | ✓ |

F-Droid doesn't allow the non-free Dropbox and Google libraries, and Flathub builds the app from public sources, so the private keys these services require can't be included. To sync these versions with your other devices, use a reader service, or install the Google Play version on Android or the `.deb`, `.rpm` or AppImage package on Linux.
