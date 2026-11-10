---
title: "How do I import or export my feeds and articles?"
weight: 50
---

Open **Import and export** (Settings → Feeds & accounts, or **File → Import and
export** on desktop).

- **Subscriptions (OPML)**: import the OPML file exported by another reader, or
  export your feeds to move them elsewhere. Every feed in the file is added,
  even if it can't be reached at the moment. Feeds that fail to update are
  marked with a warning icon, and you can fix or delete them in **Settings →
  Feeds & accounts → Feeds**.
- **Articles (CSV)**: export all, read, unread or bookmarked articles, and
  import them on another device. Import your feeds first: articles can only be
  imported for feeds that already exist.

If FeedFlow says it **can't import this file**, the file isn't valid OPML. A
common cause is an XML comment containing `--`, which is not allowed in XML.
