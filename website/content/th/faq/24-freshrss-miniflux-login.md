---
title: "Which password do I use for FreshRSS or Miniflux?"
weight: 240
---

FeedFlow connects through the Google Reader API, which uses its own password:

- **FreshRSS**: set an API password in FreshRSS under **Settings → Profile**,
  and use your instance's API address as the server URL.
- **Miniflux**: enable the Google Reader API under **Settings → Integrations →
  Google Reader** and use the username and password set there. API tokens aren't
  used.

If login succeeds but sync fails with “the server rejected the sync token”, your
server is probably behind a reverse proxy that doesn't forward the
`Authorization` header. Configure the proxy to pass it through.

Servers with a self-signed certificate aren't supported.
