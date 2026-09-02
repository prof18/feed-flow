package com.prof18.feedflow.shared.domain

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
    val contentWithSubtitle = subtitleTag + content

    val processedContent = if (imageUrl != null && !hasLeadingImage(content)) {
        val heroTag = "<img class=\"__hero\" src=\"${imageUrl.escapeHtml()}\" alt=\"\" />"
        val h4CloseIndex = contentWithSubtitle.indexOf("</h4>", ignoreCase = true)
        if (h4CloseIndex >= 0) {
            val insertAt = h4CloseIndex + "</h4>".length
            contentWithSubtitle.substring(0, insertAt) + heroTag + contentWithSubtitle.substring(insertAt)
        } else {
            heroTag + contentWithSubtitle
        }
    } else {
        contentWithSubtitle
    }

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
            $processedContent
        </div>
    </div>
    <script>
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
