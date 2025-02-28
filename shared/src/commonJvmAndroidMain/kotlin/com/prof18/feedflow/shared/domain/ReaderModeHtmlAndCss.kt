package com.prof18.feedflow.shared.domain


fun getReaderModeStyledHtmlWithParser(
    colors: ReaderColors?,
    readabilityJS: String?,
    title: String?,
    fontSize: Int,
): String {
    val titleTag = if (title != null) {
        "<h1>$title</h1>"
    } else {
        ""
    }
    // language=html
    return """
    <html lang="en">
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style>
      ${readerModeCss(colors, fontSize)}
    </style>
    <script>$readabilityJS</script>
    <body>
    $titleTag
    <div id="container">
    </div>    
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

fun getReaderModeStyledHtml(
    colors: ReaderColors?,
    content: String,
    title: String?,
    fontSize: Int,
): String {
    val titleTag = if (title != null) {
        "<h1>$title</h1>"
    } else {
        ""
    }
    // language=html
    return """
    <html lang="en">
    <style>
      ${readerModeCssOld(colors, fontSize)}
    </style>
    <body>
    $titleTag
    <div id="container">
    $content
    </div>    
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

internal fun readerModeCss(colors: ReaderColors?, fontSize: Int): String {
    val fontSizeCss = "${fontSize}px"
    // language=css
    return """
        html, body {
            margin: 0;
        }
        
        body {
           padding-top: 16px;
            padding-left: 16px;
            padding-right: 16px;
            ${colors?.let { "color: ${it.textColor};" }}
            overflow-wrap: break-word;
            font: -apple-system-body;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
            font-size: $fontSizeCss;
            line-height: 1.5em;
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

        img, iframe, object, video {
            max-width: 100%;
            height: auto;
            border-radius: 7px;
        }

        pre {
            max-width: 100%;
            overflow-x: auto;
        }

        table {
            display: block;
            max-width: 100%;
            overflow-x: auto;
        }

        a:link {
            ${colors?.let { "color: ${it.linkColor};" }}
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

        __content {
            padding: 1.5em;
            margin: auto;
            margin-top: 5px;
            max-width: 700px;
        }

        @media (prefers-color-scheme: dark) {
            body {
                ${colors?.let { "color: ${it.linkColor};" }}
                background-color: \(bgDark);
            }
            a:link { 
                ${colors?.let { "color: ${it.linkColor};" }} 
            }
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
            ${colors?.let { "color: ${it.linkColor};" }}
            min-height: 44px;
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            font-size: 1em;
            border: none;
            border-radius: 0.5em;
        }

        @media (prefers-color-scheme: dark) {
            #__footer > button {
                color: \(fg2Dark);
            }
        }

        </style>
    """.trimIndent()
}

internal fun readerModeCssOld(colors: ReaderColors?, fontSize: Int): String {
    val fontSizeCss = "${fontSize}px"
    // language=css
    return """
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
      ${colors?.let { "color: ${it.textColor};" }}
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
      font-size: $fontSizeCss;
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
      ${colors?.let { "color: ${it.linkColor};" }}
    }
    ul {
      list-style: none;
      padding-left: 8px;
    }
    ul li::before {
      content: "\2022";
      ${colors?.let { "color: ${it.textColor};" }}
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
      ${colors?.let { "color: ${it.textColor};" }}
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
}

data class ReaderColors(
    val textColor: String,
    val linkColor: String,
)
