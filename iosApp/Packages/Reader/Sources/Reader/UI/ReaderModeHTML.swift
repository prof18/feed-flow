import Foundation
import Fuzi
import SwiftSoup

extension Reader {
    // swiftlint:disable:next function_body_length
    static func wrapHTMLInReaderStyling(
        html: String,
        title: String,
        baseURL: URL?,
        heroImage: URL?,
        additionalCSS: String?,
        includeExitReaderButton _: Bool = true
    ) -> String {
        let escapedTitle = Entities.escape(title.byStrippingSiteNameFromPageTitle)
        let logger = Reader.logger

        let (fgLight, fgDark) = ReaderTheme.foreground.hexPair
        let (fg2Light, fg2Dark) = ReaderTheme.foreground2.hexPair
        let (bgLight, bgDark) = ReaderTheme.background.hexPair
        let (bg2Light, bg2Dark) = ReaderTheme.background2.hexPair
        let (linkLight, linkDark) = ReaderTheme.link.hexPair

        let heroHTML: String = {
            if let heroImage = heroImage {
                do {
                    let firstImageIndex = try estimateLinesUntilFirstImage(html: html)
                    logger.info("First image index: \(firstImageIndex ?? 999)")
                    // If there is no image in the first 10 elements, insert the hero image:
                    if (firstImageIndex ?? 999) > 10 {
                        let safeURL = Entities.escape(heroImage.absoluteString)
                        return "<img class='__hero' src=\"\(safeURL)\" />"
                    }
                } catch {
                    logger.error("\(error)")
                }
            }
            return ""
        }()

        let subtitle: String = {
            var partsHTML = [String]()

            let separatorHTML = "<span class='__separator'> · </span>"
            func appendSeparatorIfNecessary() {
                if partsHTML.count > 0 {
                    partsHTML.append(separatorHTML)
                }
            }
            if let host = baseURL?.hostWithoutWWW {
                appendSeparatorIfNecessary()
                partsHTML.append(host)
            }
            if partsHTML.count == 0 { return "" }
            return "<p class='__subtitle'>\(partsHTML.joined())</p>"
        }()

        let wrapped = """
            <!DOCTYPE html dir='auto'>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>\(escapedTitle)</title>
            <style>

            html, body {
                margin: 0;
            }

            body {
                color: \(fgLight);
                background-color: \(bgLight);
                overflow-wrap: break-word;
                font: -apple-system-body;
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
                font-family: -apple-system;
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
                color: \(linkLight);
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

            @media (prefers-color-scheme: dark) {
                body {
                    color: \(fgDark);
                    background-color: \(bgDark);
                }
                a:link { color: \(linkDark); }
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
                background-color: \(bg2Light);
                font-weight: 500;
                color: \(fg2Light);
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
                    background-color: \(bg2Dark);
                    color: \(fg2Dark);
                }
            }

            \(additionalCSS ?? "")
        
            </style>
            <body>
            <div id='__content' style='opacity: 0'>
                \(heroHTML)

                <h1>\(escapedTitle)</h1>
                    \(subtitle)
                <div id="__reader_container">\(html)</div>
            </div>

            <script>
                setTimeout(() => {
                    document.getElementById('__content').style.opacity = 1;
                }, 100);
            </script>

            </body>
        """
        return wrapped
    }
}

extension URL {
    /// If HTML is generated with `includeExitReaderButton=true`, clicking the button will navigate to this URL,
    /// which you should intercept and use to display the original website.
    static let exitReaderModeLink = URL(string: "feeeed://exit-reader-mode")!
}

private func estimateLinesUntilFirstImage(html: String) throws -> Int? {
    let doc = try HTMLDocument(data: html.data(using: .utf8)!)
    var lines = 0
    var linesBeforeFirst: Int?
    try doc.root?.traverse { element in
        if element.tag?.lowercased() == "img", linesBeforeFirst == nil {
            linesBeforeFirst = lines
        }
        lines += element.estLineCount
    }
    return linesBeforeFirst
}

extension Fuzi.XMLElement {
    func traverse(_ block: (Fuzi.XMLElement) -> Void) throws {
        for child in children {
            block(child)
            try child.traverse(block)
        }
    }

    var estLineCount: Int {
        if let tag = tag?.lowercased() {
            switch tag {
            case "video", "embed": return 5
            case "h1", "h2", "h3", "h4", "h5", "h6", "p", "li":
                return Int(ceil(Double(stringValue.count) / 60)) + 1
            case "tr": return 1
            default: return 0
            }
        }
        return 0
    }
}
