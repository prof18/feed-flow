import Foundation

enum Reader {
    static var logger: Logger = PrintLogger()

    static func warmup() {
        MercuryExtractor.shared.warmUp()
    }

    static func extractArticleContent(
        url: URL,
        html: String
    ) async throws -> ExtractedContent {
        return try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.main.async {
                MercuryExtractor.shared.extract(html: html, url: url) { contentOpt in
                    if let content = contentOpt {
                        continuation.resume(returning: content)
                    } else {
                        continuation.resume(throwing: ExtractionError.failedToExtract)
                    }
                }
            }
        }
    }

    static func fetchAndExtractContent(
        fromURL url: URL,
        additionalCSS: String?
    ) async throws -> FetchAndExtractionResult {
        DispatchQueue.main.async { Reader.warmup() }

        let (data, response) = try await URLSession.shared.data(from: url)
        let html = String(decoding: data, as: UTF8.self)
        let baseURL = response.url ?? url
        let content = try await Reader.extractArticleContent(url: baseURL, html: html)
        guard let extractedHTML = content.content else {
            throw ExtractionError.missingExtractionData
        }
        let extractedMetadata = try? await SiteMetadata.extractMetadata(
            fromHTML: html, baseURL: baseURL
        )
        let styledHTML = Reader.wrapHTMLInReaderStyling(
            html: extractedHTML,
            title: content.title ?? extractedMetadata?.title ?? "",
            baseURL: baseURL,
            heroImage: extractedMetadata?.heroImage,
            additionalCSS: additionalCSS,
            includeExitReaderButton: true,
            direction: content.direction
        )
        return FetchAndExtractionResult(
            metadata: extractedMetadata,
            extracted: content,
            styledHTML: styledHTML,
            baseURL: baseURL
        )
    }
}
