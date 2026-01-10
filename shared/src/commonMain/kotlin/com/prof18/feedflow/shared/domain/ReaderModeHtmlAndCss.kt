package com.prof18.feedflow.shared.domain

// Last export: 2025-12-21T11:48:48.756Z
fun getReaderModeStyledHtml(
    colors: ReaderColors?,
    content: String,
    fontSize: Int,
    title: String? = null, // This is added only on desktop
): String {
    val titleTag = if (title != null) {
        "<h1>$title</h1>"
    } else {
        ""
    }

    // language=html
    return """
    <html lang="en" dir='auto'>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
      ${readerModeCss(colors, fontSize)}
    </style>
    </head>
    <body>
    $titleTag
    <div id="container">
        <div id="__content">
            $content
        </div>
    </div>
    <script>
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

          document.body.addEventListener("click", function(event) {
              let anchor = event.target.closest("a");
              if (anchor) {
                  let url = anchor.getAttribute("href");
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

internal fun readerModeCss(colors: ReaderColors?, fontSize: Int): String {
    val fontSizeCss = "${fontSize}px"
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

body {
    overflow-wrap: break-word;
    font: -apple-system-body;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
    font-size: $fontSizeCss;
    line-height: 1.5em;
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
    line-height: 1.5;
    overflow-x: hidden;
}

@media screen and (min-width: 650px) {
    #__content {  line-height: 1.5; }
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

img, iframe, object, video {
    max-width: 100%;
    height: auto;
    border-radius: 7px;
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

iframe {
    width: 100%;
    max-width: 100%;
    height: 250px;
    max-height: 250px;
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

img, iframe, object, video {
    max-width: 100%;
    height: auto;
    border-radius: 7px;
}

    """.trimIndent()
}

data class ReaderColors(
    val textColor: String,
    val linkColor: String,
    val backgroundColor: String,
    val borderColor: String? = null,
)
