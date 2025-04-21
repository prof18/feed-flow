package com.prof18.feedflow.shared.domain

import android.text.TextUtils.replace
import androidx.core.net.toUri

fun getReaderModeStyledHtml(
    colors: ReaderColors,
    articleLink: String,
    content: String,
    title: String?,
    heroImageUrl: String?,
    fontSize: Int,
): String {
    val cleanedTitleCode = title?.strippingSiteNameFromPageTitle()?.let { pageTitle ->
        "<h1>$pageTitle</h1>"
    }.orEmpty()

    val subtitleCode = articleLink.toUri().host
        ?.replace("www.", "").let { subtitle ->
            "<p class='__subtitle'>$subtitle</p>"
        }

    val heroImageCode = heroImageUrl?.let { url ->
        "<img class='__hero' src=\"$url\" />"
    }.orEmpty()

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
                color: ${colors.textColor};
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
            }

            table {
                display: block;
                max-width: 100%;
                overflow-x: auto;
            }

            a:link {
                color: ${colors.linkColor};
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
                padding: 1.5em;
                margin: auto;
                margin-top: 5px;
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
            
            pre {
             line-height: 1.4286;
            }
            
            code {
              padding: 1px 2px;
              line-height: 1.8em;
              border-radius: 2px;
            }
            
            pre code {
              letter-spacing: -.027em;
              font-size: 1.15em;
            }
            
        </style>
        <body>
            <div id="__reader_container">
                <div id='__content' style='opacity: 0'>
                    $heroImageCode
        
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
