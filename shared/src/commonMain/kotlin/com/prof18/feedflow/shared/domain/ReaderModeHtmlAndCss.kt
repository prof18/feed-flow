package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.AudioEnclosure
import com.prof18.feedflow.core.model.ReaderModeDefaults

// Last export: 2025-12-21T11:48:48.756Z
fun getReaderModeStyledHtml(
    colors: ReaderColors?,
    content: String,
    fontSize: Int,
    lineHeight: Int = ReaderModeDefaults.LINE_HEIGHT,
    title: String? = null,
    imageUrl: String? = null,
    leadingContent: String = "",
    siteName: String? = null,
    audioBanner: ReaderAudioBanner? = null,
): String {
    val titleTag = if (title != null) {
        "<h1>${title.escapeHtml()}</h1>"
    } else {
        ""
    }

    val subtitleTag = if (!siteName.isNullOrBlank()) {
        "<h4>${siteName.escapeHtml()}</h4>"
    } else {
        ""
    }
    val heroTag = if (imageUrl != null && !hasLeadingImage(content)) {
        "<img class=\"__hero\" src=\"${imageUrl.escapeHtml()}\" alt=\"\" />"
    } else {
        ""
    }
    val processedContent = subtitleTag + heroTag + content
    val audioBannerTag = audioBanner?.let(::renderAudioBanner).orEmpty()

    // language=html
    return """
    <html lang="en" dir='auto'>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style>
      ${readerModeCss(colors, fontSize, lineHeight)}
    </style>
    </head>
    <body>
    $leadingContent
    $titleTag
    <div id="container">
        <div id="__content">
            $audioBannerTag
            $processedContent
        </div>
    </div>
    <script>
        window.feedflowUpdateAudioState = function(isPlaying) {
            var button = document.getElementById("reader_play_audio");
            if (!button) return;
            var label = button.getAttribute(isPlaying ? "data-pause-label" : "data-play-label");
            if (label) button.setAttribute("aria-label", label);
            var path = button.querySelector("svg path");
            if (path) path.setAttribute("d", isPlaying ? "M6 5h4v14H6zM14 5h4v14h-4z" : "M8 5v14l11-7z");
        };
        // Instagram sends JSON MEASURE messages as its media and caption finish loading.
        window.addEventListener("message", function(event) {
            if (event.origin !== "https://www.instagram.com") return;

            var payload;
            try {
                payload = JSON.parse(event.data);
            } catch (error) {
                return;
            }
            if (!payload || payload.type !== "MEASURE") return;

            var height = Number(payload.details && payload.details.height);
            if (!Number.isFinite(height) || height <= 0 || height > 10000) return;

            var instagramFrames = document.querySelectorAll(
                'iframe[src^="https://www.instagram.com/"]'
            );
            var sourceFrame = Array.prototype.find.call(instagramFrames, function(frame) {
                return frame.contentWindow === event.source;
            });
            if (!sourceFrame) return;

            sourceFrame.style.height = Math.ceil(height) + "px";
        });

        window.addEventListener("message", function(event) {
            if (event.origin !== "https://platform.twitter.com") return;

            var payload = event.data && event.data["twttr.embed"];
            if (!payload || payload.method !== "twttr.private.resize") return;

            var dimensions = payload.params && payload.params[0];
            var height = Number(dimensions && dimensions.height);
            if (!Number.isFinite(height) || height <= 0 || height > 10000) return;

            var twitterFrames = document.querySelectorAll(
                'iframe[src^="https://platform.twitter.com/embed/Tweet.html"]'
            );
            var sourceFrame = Array.prototype.find.call(twitterFrames, function(frame) {
                return frame.contentWindow === event.source;
            });
            if (!sourceFrame) return;

            sourceFrame.style.height = Math.ceil(height) + "px";
        });

        document.addEventListener("DOMContentLoaded", function () {
            // Get the title from the first h1 (which we inject)
            var firstH1 = document.querySelector("h1");
            if (firstH1) {
                var titleText = firstH1.textContent.trim().toLowerCase();
                // Check all h1 and h2 elements for duplicates
                document.querySelectorAll("h1, h2").forEach(function(el) {
                    // Skip the first h1 (our injected title)
                    if (el === firstH1) return;
                    var elText = el.textContent.trim().toLowerCase();
                    // Hide if text matches the title
                    if (elText === titleText) {
                        el.style.display = 'none';
                    }
                });
            }

          function hideBrokenImage(image) {
              image.classList.add("__feedflow_image_load_failed");
              image.setAttribute("aria-hidden", "true");
          }

          document.querySelectorAll("img").forEach(function(image) {
              image.addEventListener("error", function() {
                  hideBrokenImage(image);
              });

              if (image.complete && image.naturalWidth === 0) {
                  hideBrokenImage(image);
              }
          });

          document.body.addEventListener("click", function(event) {
              let anchor = event.target.closest("a");
              if (anchor) {
                  let url = anchor.href || anchor.getAttribute("href");
                  if (url && window.kmpJsBridge && window.kmpJsBridge.callNative) {
                      event.preventDefault();
                      window.kmpJsBridge.callNative(
                       "urlInterceptor",
                        url,
                        {}
                      );
                  }
                  return;
              }

              let image = event.target.closest("img");
              if (!image) return;

              let imageUrl = image.currentSrc ||
                  image.getAttribute("src") ||
                  image.getAttribute("data-src") ||
                  image.getAttribute("data-lazy-src") ||
                  image.getAttribute("data-original") ||
                  "";
              if (!imageUrl) return;

              // Validate URL for security - only allow http(s) URLs
              let isValidUrl = imageUrl.startsWith("http://") || imageUrl.startsWith("https://");
              let isLocalhost = imageUrl.includes("localhost") ||
                               imageUrl.includes("127.0.0.1") ||
                               imageUrl.includes("0.0.0.0") ||
                               imageUrl.includes("::1");

              if (!isValidUrl || isLocalhost) {
                  return;
              }

              event.preventDefault();
              if (window.kmpJsBridge && window.kmpJsBridge.callNative) {
                  window.kmpJsBridge.callNative(
                   "imageInterceptor",
                    imageUrl,
                    {}
                  );
              } else {
                  let encodedUrl = encodeURIComponent(imageUrl);
                  window.location.href = "feedflow-image://?src=" + encodedUrl;
              }
          });
        });
    </script>
    </body>
    </html>
        """
        .trimIndent()
}

private fun renderAudioBanner(banner: ReaderAudioBanner): String {
    if (banner.enablePlayback) return renderPlayableAudioBanner(banner)
    val title = banner.title?.takeIf { it.isNotBlank() } ?: banner.untitledAudioLabel
    val audioLabel = banner.audioLabel.escapeHtml()
    val escapedTitle = title.escapeHtml()
    val openAudioLabel = banner.openAudioLabel.escapeHtml()
    val accessibleLabel = "$audioLabel: $escapedTitle. $openAudioLabel"

    return """
        <a id="reader_audio_banner" class="__audio_banner"
            href="${AudioEnclosure.ACTION_URL}" aria-label="$accessibleLabel">
            <svg class="__audio_banner_icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M3 14v-3a9 9 0 0 1 18 0v3" />
                <path d="M3 13h3v7H5a2 2 0 0 1-2-2v-5Zm18 0h-3v7h1a2 2 0 0 0 2-2v-5Z" />
            </svg>
            <span class="__audio_banner_copy">
                <span class="__audio_banner_label">$audioLabel</span>
                <span class="__audio_banner_title">$escapedTitle</span>
            </span>
            <span id="reader_open_audio" class="__audio_banner_action">
                $openAudioLabel
                <svg class="__audio_banner_arrow" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                    <path d="M6 3h7v7M13 3 5 11M11 9v4H3V5h4" />
                </svg>
            </span>
        </a>
    """.trimIndent()
}

private fun renderPlayableAudioBanner(banner: ReaderAudioBanner): String {
    val title = (banner.title?.takeIf { it.isNotBlank() } ?: banner.untitledAudioLabel).escapeHtml()
    val subtitle = banner.subtitle?.takeIf { it.isNotBlank() }?.escapeHtml() ?: banner.audioLabel.escapeHtml()
    val artwork = AudioEnclosure.validatedUrl(banner.imageUrl)?.let {
        "<img class=\"__audio_artwork\" src=\"${it.escapeHtml()}\" alt=\"\" />"
    }.orEmpty()
    return """
        <aside id="reader_audio_banner" class="__audio_banner __audio_player_card"
            aria-label="${banner.audioLabel.escapeHtml()}">
            <span class="__audio_artwork_container">
                <svg class="__audio_banner_icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                    <path d="M3 14v-3a9 9 0 0 1 18 0v3" />
                    <path d="M3 13h3v7H5a2 2 0 0 1-2-2v-5Zm18 0h-3v7h1a2 2 0 0 0 2-2v-5Z" />
                </svg>
                $artwork
            </span>
            <span class="__audio_banner_copy">
                <span class="__audio_banner_title">$title</span>
                <span class="__audio_banner_label">$subtitle</span>
            </span>
            <a id="reader_play_audio" class="__audio_play" href="${AudioEnclosure.PLAY_ACTION_URL}"
                data-play-label="${banner.playAudioLabel.escapeHtml()}: $title"
                data-pause-label="${banner.pauseAudioLabel.escapeHtml()}: $title"
                role="button" aria-label="${banner.playAudioLabel.escapeHtml()}: $title">
                <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M8 5v14l11-7z" /></svg>
                <span>${banner.playAudioLabel.escapeHtml()}</span>
            </a>
            <a id="reader_open_audio" class="__audio_banner_action __audio_external"
                href="${AudioEnclosure.ACTION_URL}">${banner.openAudioLabel.escapeHtml()} ↗</a>
        </aside>
    """.trimIndent()
}

private fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

private fun hasLeadingImage(content: String): Boolean {
    val imageIndex = content.indexOf("<img", ignoreCase = true)
    if (imageIndex < 0) return false

    val visiblePrefix = content.substring(0, imageIndex)
        .replace(HTML_HEADING_REGEX, " ")
        .replace(HTML_ELEMENT_REGEX, " ")
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace(HTML_WHITESPACE_REGEX, " ")
        .trim()
    return visiblePrefix.length <= MAX_LEADING_IMAGE_PREFIX_LENGTH
}

private const val MAX_LEADING_IMAGE_PREFIX_LENGTH = 200
private val HTML_HEADING_REGEX = Regex(
    """<h[1-6]\b[^>]*>.*?</h[1-6]\s*>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val HTML_ELEMENT_REGEX = Regex("<[^>]*>")
private val HTML_WHITESPACE_REGEX = Regex("\\s+")

internal fun readerModeCss(colors: ReaderColors?, fontSize: Int, lineHeight: Int): String {
    val fontSizeCss = "${fontSize}px"
    val lineHeightCss = readerLineHeightToCss(lineHeight)
    val textColor = colors?.textColor ?: "inherit"
    val linkColor = colors?.linkColor ?: "inherit"
    val backgroundColor = colors?.backgroundColor ?: "transparent"
    val borderColor = colors?.borderColor ?: "transparent"
    // language=css
    return """
:root {
    --reader-text: $textColor;
    --reader-link: $linkColor;
    --reader-bg: $backgroundColor;
    --reader-border: $borderColor;
}

html {
    overflow-x: hidden;
}

body {
    overflow-x: hidden;
    overflow-wrap: break-word;
    font: -apple-system-body;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
    font-size: $fontSizeCss;
    line-height: $lineHeightCss;
    padding-bottom: 112px;
    color: var(--reader-text);
}

.__hero {
    display: block;
    width: 100%;
    height: 50vw;
    max-height: 300px;
    object-fit: cover;
    overflow: hidden;
    border-radius: 7px;
}

#__content {
    line-height: $lineHeightCss;
    overflow-x: hidden;
}

@media screen and (min-width: 650px) {
    #__content {  line-height: $lineHeightCss; }
}

h1, h2, h3, h4, h5, h6 {
    line-height: 1.2;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
    font-weight: 800;
}

body > h1 {
    padding: 0 16px;
    margin: 16px auto;
    max-width: 700px;
}

img, object, video {
    max-width: 100%;
    height: auto;
    border-radius: 7px;
}

iframe {
    width: 100%;
    max-width: 100%;
    border-radius: 7px;
}

#__content img {
    display: block;
    margin: 4px auto;
}

img.__feedflow_image_load_failed {
    display: none !important;
}

pre {
    max-width: 100%;
    overflow-x: auto;
    background-color: var(--reader-bg);
    border: 1px solid var(--reader-border);
    border-radius: 6px;
    padding: 12px 16px;
    margin: 16px 0;
    font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
    line-height: 1.4286;
    font-size: $fontSizeCss;
}

table {
    display: block;
    max-width: 100%;
    overflow-x: auto;
}

blockquote {
    margin: 1.5em 0;
    padding: 1em 1.5em;
    border-left: 4px solid var(--reader-border);
    background-color: var(--reader-bg);
    border-radius: 0 6px 6px 0;
    font-style: italic;
    position: relative;
}

blockquote p {
    margin: 0.5em 0;
}

blockquote p:first-child {
    margin-top: 0;
}

blockquote p:last-child {
    margin-bottom: 0;
}

blockquote cite {
    display: block;
    text-align: right;
    margin-top: 1em;
    font-style: normal;
    font-weight: 600;
    opacity: 0.7;
}

blockquote cite:before {
    content: "— ";
}

a:link {
    color: var(--reader-link);
}

figure {
    margin-left: 0;
    margin-right: 0;
}

figcaption, cite {
    opacity: 0.5;
    font-size: small;
}

aside.callout[data-callout] {
    margin: 1.75em 0;
    padding: 1em;
    border: 1px solid color-mix(in srgb, var(--reader-link) 22%, transparent);
    border-left: 0.3em solid var(--reader-link);
    border-radius: 10px;
    background-color: color-mix(in srgb, var(--reader-link) 7%, transparent);
}

aside.callout[data-callout] .callout-content {
    display: grid;
    grid-template-columns: 96px minmax(0, 1fr);
    gap: 1em;
    align-items: center;
}

aside.callout[data-callout] .callout-media {
    display: block;
}

aside.callout[data-callout] .callout-media img {
    width: 96px;
    height: 96px;
    margin: 0;
    object-fit: cover;
}

aside.callout[data-callout] .callout-title {
    margin: 0.25em 0;
    font-size: 1em;
    line-height: 1.3;
}

aside.callout[data-callout] .callout-label {
    font-size: 0.75em;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    opacity: 0.7;
}

.__audio_banner {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: 12px;
    margin: 0 0 16px;
    padding: 16px;
    border: 1px solid var(--reader-border);
    border-radius: 12px;
    background: color-mix(in srgb, var(--reader-link) 7%, var(--reader-bg));
    color: inherit;
    text-align: start;
    text-decoration: none;
}

.__audio_banner:focus-visible {
    outline: 3px solid var(--reader-link);
    outline-offset: 3px;
}

.__audio_banner_icon {
    width: 24px;
    height: 24px;
    fill: none;
    stroke: var(--reader-link);
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 1.8;
}

.__audio_banner_copy {
    display: grid;
    min-width: 0;
    gap: 2px;
    overflow-wrap: anywhere;
}

.__audio_banner_label {
    font-size: 0.75em;
    letter-spacing: 0.04em;
    opacity: 0.7;
}

.__audio_banner_title {
    min-width: 0;
    overflow-wrap: anywhere;
    font-weight: 600;
}

.__audio_banner_action {
    display: inline-flex;
    min-width: 0;
    min-height: 44px;
    align-items: center;
    justify-content: flex-end;
    gap: 6px;
    overflow-wrap: anywhere;
    color: var(--reader-link);
    font-weight: 600;
    text-align: start;
}

.__audio_banner_arrow {
    flex: none;
    width: 16px;
    height: 16px;
    fill: none;
    stroke: currentColor;
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 1.5;
}

.__audio_player_card {
    grid-template-columns: auto minmax(0, 1fr) auto;
}

.__audio_artwork_container {
    display: grid;
    place-items: center;
    width: 56px;
    height: 56px;
    overflow: hidden;
    border-radius: 10px;
    background: color-mix(in srgb, var(--reader-link) 12%, var(--reader-bg));
}

.__audio_artwork_container > * { grid-area: 1 / 1; }
.__audio_artwork { width: 100%; height: 100%; object-fit: cover; }
.__audio_play {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    color: var(--reader-bg);
    background: var(--reader-link);
}
.__audio_play svg { width: 24px; height: 24px; fill: var(--reader-bg); }
.__audio_play span { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
.__audio_play:focus-visible, .__audio_external:focus-visible { outline: 3px solid var(--reader-link); outline-offset: 3px; }
.__audio_external { grid-column: 1 / -1; justify-content: flex-start; border-top: 1px solid var(--reader-border); }

@media (max-width: 420px) {
    .__audio_banner {
        grid-template-columns: auto minmax(0, 1fr);
    }

    .__audio_banner_action {
        grid-column: 2;
        justify-content: flex-start;
    }
    .__audio_player_card { grid-template-columns: auto minmax(0, 1fr) auto; gap: 10px; padding: 12px; }
    .__audio_player_card .__audio_external { grid-column: 1 / -1; }
    .__audio_player_card .__audio_artwork_container { width: 44px; height: 44px; }
}

.__subtitle {
    font-weight: bold;
    vertical-align: baseline;
    opacity: 0.5;
}

.__subtitle .__icon {
    width: 1.2em;
    height: 1.2em;
    object-fit: cover;
    overflow: hidden;
    border-radius: 3px;
    margin-right: 0.3em;
    position: relative;
    top: 0.3em;
}

.__subtitle .__separator {
    opacity: 0.5;
}

#__content {
    padding: 0 16px 16px 16px;
    margin: auto;
    max-width: 700px;
}

#__footer {
    margin-bottom: 4em;
    margin-top: 2em;
}

#__footer > .label {
    font-size: small;
    opacity: 0.5;
    text-align: center;
    margin-bottom: 0.66em;
    font-weight: 500;
}

#__footer > button {
    padding: 0.5em;
    text-align: center;
    font-weight: 500;
    min-height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    font-size: 1em;
    border: none;
    border-radius: 0.5em;
}

iframe[src^="https://www.youtube-nocookie.com/embed/"],
iframe[src^="https://www.youtube.com/embed/"],
iframe[src^="https://player.vimeo.com/video/"] {
    aspect-ratio: 16 / 9;
    height: auto;
    border: 0;
}

iframe[src^="https://platform.twitter.com/embed/Tweet.html"] {
    border: 0;
}

code {
    padding: 2px 4px;
    border-radius: 3px;
    line-height: 1.4em;
    background-color: var(--reader-bg);
    border: 1px solid var(--reader-border);
    font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
    font-size: $fontSizeCss;
    color: var(--reader-text);
}

pre code {
    letter-spacing: -.027em;
    font-size: $fontSizeCss;
    background-color: transparent;
    border: none;
    padding: 0;
}

    """.trimIndent()
}

private const val LINE_HEIGHT_BASE_TENTHS = 15
private const val LINE_HEIGHT_STEP_TENTHS = 1
private const val LINE_HEIGHT_TENTHS_DIVISOR = 10
private const val LINE_HEIGHT_DESKTOP_ROUNDING_OFFSET = 5

// step 0 -> "1.5", default step 1 -> "1.6", step 15 -> "3.0". Integer tenths avoids float/locale issues.
internal fun readerLineHeightToCss(step: Int): String {
    val tenths = LINE_HEIGHT_BASE_TENTHS + step * LINE_HEIGHT_STEP_TENTHS
    return "${tenths / LINE_HEIGHT_TENTHS_DIVISOR}.${tenths % LINE_HEIGHT_TENTHS_DIVISOR}"
}

fun readerLineHeightToTextLineHeightSp(fontSize: Int, step: Int): Int =
    (
        fontSize * (LINE_HEIGHT_BASE_TENTHS + step * LINE_HEIGHT_STEP_TENTHS) +
            LINE_HEIGHT_DESKTOP_ROUNDING_OFFSET
        ) / LINE_HEIGHT_TENTHS_DIVISOR

fun readerFontSizeJs(fontSize: Int): String =
    """document.getElementById("container").style.fontSize = "$fontSize" + "px";"""

fun readerLineHeightLabel(step: Int): String = readerLineHeightToCss(step)

fun readerCodeBlockColors(isDarkMode: Boolean): ReaderCodeBlockColors = if (isDarkMode) {
    ReaderCodeBlockColors(backgroundColor = "#1e1e1e", borderColor = "#444444")
} else {
    ReaderCodeBlockColors(backgroundColor = "#f6f8fa", borderColor = "#d1d9e0")
}

// Live update injected into the reader WebView (Android & iOS use the same rule string).
fun readerLineHeightJs(step: Int): String {
    val lineHeight = readerLineHeightToCss(step)
    return """
        (function() {
          var styleId = "__feedflow_line_height_style";
          var style = document.getElementById(styleId);
          if (!style) {
            style = document.createElement("style");
            style.id = styleId;
            document.head.appendChild(style);
          }
          style.textContent = "body, #__content { line-height: $lineHeight; }";
        })();
    """.trimIndent()
}

data class ReaderColors(
    val textColor: String,
    val linkColor: String,
    val backgroundColor: String,
    val borderColor: String? = null,
)

data class ReaderCodeBlockColors(
    val backgroundColor: String,
    val borderColor: String,
)
