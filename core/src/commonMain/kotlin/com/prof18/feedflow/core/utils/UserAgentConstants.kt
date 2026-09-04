package com.prof18.feedflow.core.utils

// Identifies the app to arbitrary third-party feed and article hosts. Some WAFs (e.g. Cloudflare)
// treat a versionless, contact-URL crawler UA more leniently than a versioned token, especially
// from low-reputation (VPN/datacenter) IPs.
const val FEEDFLOW_USER_AGENT = "FeedFlow (RSS Reader; +https://feedflow.dev)"

// Some feed hosts reject user agents containing a URL, so feed fetching retries HTTP 403
// responses once with this form.
const val FEEDFLOW_FALLBACK_USER_AGENT = "FeedFlow (RSS Reader)"

// Article hosts may still reject app-specific user agents after a 403. This matches the browser
// client hints used by HtmlRetriever and is only sent by its fallback request.
const val FEEDFLOW_READER_FALLBACK_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

// Sync clients talk to the user's own server, where a versioned UA aids support debugging.
fun feedFlowUserAgent(appVersion: String): String = "FeedFlow/$appVersion (RSS Reader)"
