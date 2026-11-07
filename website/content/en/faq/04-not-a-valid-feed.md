---
title: "Why does FeedFlow say “The link you provided is not a valid RSS feed”?"
weight: 40
---

FeedFlow downloads the link and checks that it's a feed. The check can fail even when the feed opens in your browser or works in another reader:

- **The site blocks automated readers.** Some sites show a security check (for example Cloudflare) before sharing their content. When this happens, FeedFlow offers **Verify website**: complete the check once and FeedFlow continues on its own.
- **The site has a certificate problem.** A misconfigured or self-signed certificate makes the connection fail, even if a browser works around it. Only the site owner can fix this.
- **The link isn't a feed.** It may be a normal web page whose site doesn't advertise a feed, or a JSON Feed, which FeedFlow doesn't support yet.
- **The site limits requests or blocks your region.** Reddit, for example, limits how often its feeds can be downloaded.
- **The connection failed.** Try again in a moment.

If you're sure the feed is fine, **Add anyway** adds it without the check, and you can remove it later if it never updates.

Online services like Feedly and Inoreader download feeds on their own servers, which is why some feeds work there. FeedFlow downloads everything directly from your device.

Feeds that require a username, password or cookies aren't supported yet.

If a feed should work and doesn't, [open an issue](https://github.com/prof18/feed-flow/issues) with its link.
