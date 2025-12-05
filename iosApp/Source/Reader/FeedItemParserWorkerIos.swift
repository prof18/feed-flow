//
//  FeedItemParserWorkerIos.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 11/04/25.
//

import FeedFlowKit
import Foundation

class FeedItemParserWorkerIos: FeedItemParserWorker {
    func parse(
        feedItemId: String,
        url: String,
        completionHandler: @escaping (ParsingResult?, (any Error)?) -> Void
    ) {
        DispatchQueue.main.async { [weak self] in
            guard let this = self else { return }
            let shouldSaveContent = Deps.shared.getSettingsRepository().isSaveItemContentOnOpenEnabled()
            this.handleParsing(
                feedItemId: feedItemId,
                url: url,
                feedItemParser: FeedItemParser.shared,
                saveContent: shouldSaveContent
            ) { result in
                completionHandler(result, nil)
            }
        }
    }

    private func handleParsing(
        feedItemId: String,
        url: String,
        feedItemParser: FeedItemParser,
        saveContent: Bool,
        completionHandler: @escaping (ParsingResult) -> Void
    ) {
        parseFeedItem(url: url, feedItemParser: feedItemParser) { parsingResult in
            switch onEnum(of: parsingResult) {
            case let .success(result):
                let htmlContent = result.htmlContent
                let title = result.title

                var siteHtml = ""
                if let site = result.siteName {
                    siteHtml = "<h4>\(site)</h4>"
                }

                var htmlWithTitle: String?
                if let htmlContent = htmlContent {
                    if let title = title {
                        htmlWithTitle = "<h1>\(title)</h1>\n\(siteHtml)\n\(htmlContent)"
                    } else {
                        htmlWithTitle = htmlContent
                    }

                    if saveContent {
                        let fileURL = self.getContentPath(feedItemId: feedItemId)
                        if let data = htmlWithTitle?.data(using: .utf8) {
                            do {
                                try data.write(to: fileURL)
                            } catch {
                                Deps.shared.getLogger(tag: "FeedItemParserWorkerIos").d(
                                    messageString: "Error writing to file: \(error)"
                                )
                            }
                        }
                    }
                }

                let result = ParsingResult.Success(
                    htmlContent: htmlWithTitle,
                    title: title,
                    siteName: result.siteName
                )
                completionHandler(result)

            case .error:
                completionHandler(ParsingResult.Error())
            }
        }
    }

    private func parseFeedItem(
        url: String,
        feedItemParser: FeedItemParser,
        completionHandler: @escaping (ParsingResult) -> Void
    ) {
        Deps.shared.getHtmlRetriever().retrieveHtml(url: url) { html, error in
            if error != nil {
                completionHandler(ParsingResult.Error())
                return
            }

            guard let html = html else {
                completionHandler(ParsingResult.Error())
                return
            }

            DispatchQueue.main.async {
                feedItemParser.parseArticle(url: url, htmlContent: html) { result in
                    completionHandler(result)
                }
            }
        }
    }

    func getContentPath(feedItemId: String) -> URL {
        let articlesDirectory: URL

        if let containerURL = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: "group.com.prof18.feedflow") {
            articlesDirectory = containerURL.appendingPathComponent("articles")
        } else {
            // Fallback to regular document directory if app group fails
            articlesDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                .appendingPathComponent("articles")
        }

        // Create articles directory if it doesn't exist
        if !FileManager.default.fileExists(atPath: articlesDirectory.path) {
            do {
                try FileManager.default.createDirectory(
                    at: articlesDirectory,
                    withIntermediateDirectories: true,
                    attributes: nil
                )
            } catch {
                Deps.shared.getLogger(tag: "FeedItemParserWorkerIos").d(
                    messageString: "Error creating articles directory: \(error)"
                )
            }
        }

        return articlesDirectory.appendingPathComponent("\(feedItemId).html")
    }
}
