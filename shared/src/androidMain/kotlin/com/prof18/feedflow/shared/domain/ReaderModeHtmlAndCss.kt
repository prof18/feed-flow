package com.prof18.feedflow.shared.domain

import androidx.core.net.toUri

fun getReaderModeStyledHtml(
    colors: ReaderColors,
    articleLink: String,
    content: String,
    title: String?,
    fontSize: Int,
): String {
    val cleanedTitleCode = title?.strippingSiteNameFromPageTitle()?.let { pageTitle ->
        "<h1>$pageTitle</h1>"
    }.orEmpty()

    val subtitleCode = articleLink.toUri().host
        ?.replace("www.", "").let { subtitle ->
            "<p class='__subtitle'>$subtitle</p>"
        }

    val fontSizeCss = "${fontSize}px"

    // language=html
    return """
    <!DOCTYPE html dir='auto'>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>\(escapedTitle)</title>
        <style>
            html, body {
                margin: 0;
            }
            
            body {
                overflow-wrap: break-word;
                font: -apple-system-body;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                font-size: $fontSizeCss;
                line-height: 1.5em;
                padding-bottom: 24px;
                ${colors?.let { "color: ${it.textColor};" }}
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
            
            img, iframe, object, video {
                max-width: 100%;
                height: auto;
                border-radius: 7px;
            }
            
            pre {
                max-width: 100%;
                overflow-x: auto;
                ${colors?.let { "background-color: ${it.backgroundColor};" }}
                ${colors?.let { "border: 1px solid ${it.borderColor};" }}
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
            
            #__content {
                padding: 0 24px 24px 24px;
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
                ${colors?.let { "background-color: ${it.backgroundColor};" }}
                ${colors?.let { "border: 1px solid ${it.borderColor};" }}
                font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
                font-size: $fontSizeCss;
                ${colors?.let { "color: ${it.textColor};" }}
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
                        
        </style>
        <body>
            <div id="__reader_container">
                <div id='__content' style='opacity: 0'>
                    $cleanedTitleCode
                        $subtitleCode
                    $content
                </div>
            </div>
            
            <script>
                setTimeout(() => {
                    document.getElementById('__content').style.opacity = 1;
                }, 100);
            </script>
            
            <script>    
                document.addEventListener("DOMContentLoaded", function () {        
                  document.body.addEventListener("click", function(event) {
                      if (event.target.tagName.toLowerCase() === "a") {
                         console.log("clicked a")
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
    """.trimIndent()
}

private fun String.strippingSiteNameFromPageTitle(): String? =
    this.split(" | ", " – ", " — ", " - ")
        .firstOrNull()?.trim()
