package com.prof18.feedflow.core.utils

// Identifies the app to arbitrary third-party feed and article hosts. Some WAFs (e.g. Cloudflare)
// treat a versionless, contact-URL crawler UA more leniently than a versioned token, especially
// from low-reputation (VPN/datacenter) IPs.
const val FEEDFLOW_USER_AGENT = "FeedFlow (RSS Reader; +https://feedflow.dev)"

// Some feed hosts reject user agents containing a URL, so feed fetching retries HTTP 403
// responses once with this form.
const val FEEDFLOW_FALLBACK_USER_AGENT = "FeedFlow (RSS Reader)"

// Article hosts may still reject app-specific user agents after a 403. This browser identity
// is also used by the last feed-fetch tier.
const val FEEDFLOW_READER_FALLBACK_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

// Akamai-style bot managers score the whole header profile, not the User-Agent alone:
// finanzen.net returns 403 for a browser UA sent on its own, and for this full header
// set sent with a FeedFlow UA. Accept-Encoding is deliberately absent -- OkHttp only
// gunzips transparently when it adds that header itself, and the "gzip" it sends by
// default already satisfies the check. NSURLSession must manage compression too.
val FEEDFLOW_BROWSER_FALLBACK_HEADERS: Map<String, String> = mapOf(
    "User-Agent" to FEEDFLOW_READER_FALLBACK_USER_AGENT,
    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language" to "en-US,en;q=0.9",
    "sec-ch-ua" to "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\"",
    "sec-ch-ua-mobile" to "?0",
    "sec-ch-ua-platform" to "\"Windows\"",
    "Sec-Fetch-Dest" to "document",
    "Sec-Fetch-Mode" to "navigate",
    "Sec-Fetch-Site" to "none",
    "Sec-Fetch-User" to "?1",
    "Upgrade-Insecure-Requests" to "1",
)

// Sync clients talk to the user's own server, where a versioned UA aids support debugging.
fun feedFlowUserAgent(appVersion: String): String = "FeedFlow/$appVersion (RSS Reader)"
