package com.prof18.feedflow.android.readermode

internal fun getReaderModeHtml(
    colors: ReaderColors,
    content: String,
    title: String?,
): String {
    // language=html
    return """
    <html lang="en">
    <head>
      <title>$title</title>
    </head>
    <style>
      ${readerModeCss(colors)}
    </style>
    <body>
    ${title?.let { "<h1>$it</h1>" }}
    $content
    <script>    
        document.addEventListener("DOMContentLoaded", function () {
           document.querySelectorAll("h1")[1].style.display = 'none';

          document.body.addEventListener("click", function(event) {
              if (event.target.tagName.toLowerCase() === "a") {
                  // Prevent the default behavior of the link
                  event.preventDefault();
                  var url = event.target.getAttribute("href");
                  window.kmpJsBridge.callNative(
                   "urlInterceptor", 
                    url, 
                    {}
                  );
              }
          });
        });
    </script>
    </body>
    </html>
        """
        .trimIndent()
}

internal fun readerModeCss(colors: ReaderColors) =
    // language=css
    """
    h1 {
      font-size: 28px;
      line-height: 1.2;
    }
    h2 {
      font-size: 24px;
      line-height: 1.2;
    }
    h3, h4, h5, h6 {
      font-size: 18px;
    }
    body {
      padding-top: 16px;
      padding-left: 16px;
      padding-right: 16px;
      color: ${colors.textColor};
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
      font-size: 18px;
      line-height: 1.5em;
    }
    figure {
      margin: 0;
    }
    figcaption {
      margin-top: 8px;
      font-size: 14px;
      line-height: 1.6;
    }
    .caption {
      font-size: 12px;
    }
    .feedName {
      margin-bottom: 8px;
    }
    img, figure, video, div, object {
      max-width: 100%;
      height: auto !important;
      margin: 0 auto;
    }
    a {
      color: ${colors.linkColor};
    }
    ul {
      list-style: none;
      padding-left: 8px;
    }
    ul li::before {
      content: "\2022";
      color: ${colors.textColor};
      margin-right: 0.25em;
    }
    ul li p {
      display: inline;
    }
    ol {
      list-style: none;
      padding-left: 8px;
      counter-reset: item;
    }
    ol li::before {
      counter-increment: item;
      content: counters(item, ".") ".";
      color: ${colors.textColor};
      margin-right: 0.25em;
    }
    ol li p {
      display: inline;
    }
    li:not(:last-of-type) { 
      margin-bottom: 1em; 
    } 
    pre {
      max-width: 100%;
      margin: 0;
      overflow: auto;
      overflow-y: hidden;
      word-wrap: normal;
      word-break: normal;
      border-radius: 4px;
      padding: 8px;
    }
    pre {
      line-height: 1.4286;
    }
    code {
      padding: 1px 2px;
      border-radius: 2px;
      font-size: 16px;
    }
    pre code {
      letter-spacing: -.027em;
      font-size: 1.15em;
    }
    iframe {
      width: 100%;
      max-width: 100%;
      height: 250px;
      max-height: 250px;
    }

    """.trimIndent()

internal data class ReaderColors(
    val textColor: String,
    val linkColor: String,
)
