---
title: "How often does FeedFlow check for new articles?"
weight: 100
featured: true
---

FeedFlow has no server: every check for new articles runs on your device. It checks:

- **When you refresh**: pull down the list or use **Refresh feeds**. Only the feeds in the current view are checked, so refreshing a category or a single feed is faster.
- **When the app starts**, if **Refresh feeds on app start** is on (Settings → Sync & storage). This only happens when the app is launched from scratch, not when you switch back to it while it's still running.
- **In the background**, depending on the platform (see below).

Automatic checks skip a feed that was checked recently, to avoid flooding websites with requests. The wait is at least an hour, and it can grow up to a day for feeds that rarely change. A manual refresh always checks again. If one feed fails, the others are still updated.

**Android**: choose how often in **Background sync** (Settings → Sync & storage), from every 15 minutes to once a day, and optionally only on Wi-Fi or only while charging. Android can still delay the check when the battery is low or battery optimisation is on for FeedFlow.

**iPhone and iPad**: iOS doesn't let apps run on a schedule, so there is no Background sync setting. When you leave FeedFlow, it asks iOS to wake it up again in about two hours. iOS decides if and when that happens: usually while the device is charging and connected to Wi-Fi, and more often for apps you open regularly. It can take many hours, or not happen at all if you haven't opened FeedFlow for a while, if Low Power Mode is on, or if **Background App Refresh** is turned off for FeedFlow in the iOS Settings. Open FeedFlow to get the latest articles immediately. Feeds from a website you verified with **Verify website** are only checked while FeedFlow is open, because the website's check usually has to be passed again by then.

**Desktop**: turn on **Refresh while open** to check for new articles while FeedFlow is running.
