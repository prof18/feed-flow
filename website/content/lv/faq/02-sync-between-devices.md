---
title: "Kā sinhronizēt FeedFlow starp dažādām ierīcēm?"
weight: 20
featured: true
---

Go to **Settings → Feeds & accounts → Accounts** and connect the same account on
every device. FeedFlow supports two kinds of accounts:

- **Dublēšanas pakalpojumus**: iCloud, Dropbox un Google disku. Jūsu RSS
  plūsmas, kategorijas, lasītie raksti un grāmatzīmes tiek saglabāti jūsu
  mākoņkrātuvē esošā datnē, un pārējās ierīces to izgūst no turienes.
- **Reader services**: FreshRSS, Miniflux, BazQux and Feedbin. Your feeds and
  read status live on the server, and FeedFlow syncs with it.

You can connect one account at a time. Sync runs when feeds are refreshed and
when you close the app; on iPhone and iPad, changes are also uploaded every
couple of minutes while FeedFlow is open. **Back up now** sends your latest
changes right away. Your other devices pick up the changes the next time they
refresh, so on iPhone and iPad, where background refresh is up to iOS, open the
app to see them.

Dublēšanas un sinhronizācijas iespējas atkarīgas no tā, kur ieguvāt FeedFlow:

| Versija                                                   | iCloud | Dropbox | Google disks | Reader services |
| --------------------------------------------------------- | ------ | ------- | ------------ | --------------- |
| iPhone, iPad un Mac (App Store)                           | ✓      | ✓       | ✓            | ✓               |
| Android (Google Play)                                     |        | ✓       | ✓            | ✓               |
| Android (F-Droid)                                         |        |         |              | ✓               |
| Windows, Linux `.deb` un `.rpm` pakotnes, kā arī AppImage |        | ✓       | ✓            | ✓               |
| Linux (Flathub)                                           |        |         |              | ✓               |

F-Droid doesn't allow the non-free Dropbox and Google libraries, and Flathub
builds the app from public sources, so the private keys these services require
can't be included. To sync these versions with your other devices, use a reader
service, or install the Google Play version on Android or the `.deb`, `.rpm` or
AppImage package on Linux.
