package com.prof18.feedflow.core.utils

// Identifies the app to arbitrary third-party feed hosts. Some WAFs (e.g. Cloudflare) treat a
// versionless, contact-URL crawler UA more leniently than a versioned token, especially from
// low-reputation (VPN/datacenter) IPs, so feed fetching uses this form.
const val FEEDFLOW_USER_AGENT = "FeedFlow (RSS Reader; +https://feedflow.dev)"

// Sync clients talk to the user's own server, where a versioned UA aids support debugging.
fun feedFlowUserAgent(appVersion: String): String = "FeedFlow/$appVersion (RSS Reader)"
