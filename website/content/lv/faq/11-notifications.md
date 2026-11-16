---
title: "Why am I not getting notifications or background updates?"
weight: 110
---

FeedFlow has no server: new articles are checked on your device, in the
background. Your phone's system decides when background work can run, and
FeedFlow can't override it.

To get notifications:

1. On Android, **Background refresh** must be set to a frequency other than
   **Never**. You can change it in **Settings → Notifications** or **Settings →
   Sync & refresh**, and **Settings → Notifications** shows a warning while it's
   **Never**. If it's **Never** when you turn on notifications, FeedFlow sets it
   to **Every hour** and tells you.
2. Turn on notifications in **Settings → Notifications**, for all feeds or for
   the feeds you choose, or with **Enable notifications for new articles** when
   you add a feed or open its **Feed settings**. Allow FeedFlow to send
   notifications when asked.
3. Pick a **Notification style**: per article (with titles and images), by feed,
   by category, or all combined.

**Android**: make sure battery optimisation is off for FeedFlow. Some phones,
such as Huawei, Honor, Xiaomi and Samsung, also need FeedFlow to be allowed to
run in the background or to auto-start.

**iPhone and iPad**: notifications can only be sent when iOS lets FeedFlow check
for new articles in the background, which happens at most every couple of hours
and only when iOS decides it's a good moment (see “How often does FeedFlow check
for new articles?”). Keep **Background App Refresh** on for FeedFlow in the iOS
Settings. **Settings → Notifications** shows a warning when it's off. Delays of
hours are normal and outside FeedFlow's control.

**Desktop**: system notifications aren't available yet.
