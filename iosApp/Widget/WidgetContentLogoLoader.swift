import Foundation
import Nuke
import UIKit

enum WidgetContentLogoLoader {
    static let fallbackSymbol = "dot.radiowaves.left.and.right"

    private static let pipeline: ImagePipeline = {
        var configuration = ImagePipeline.Configuration.withDataCache(
            name: "com.prof18.feedflow.widget.logos",
            sizeLimit: 5 * 1_024 * 1_024
        )
        configuration.dataCachePolicy = .storeEncodedImages
        let sessionConfiguration = URLSessionConfiguration.default
        sessionConfiguration.urlCache = nil
        sessionConfiguration.timeoutIntervalForRequest = 2
        sessionConfiguration.timeoutIntervalForResource = 2
        configuration.dataLoader = DataLoader(configuration: sessionConfiguration)
        return ImagePipeline(configuration: configuration)
    }()

    static func imageData(
        for urlString: String?,
        pipeline: ImagePipeline = pipeline
    ) async -> Data? {
        guard !Task.isCancelled,
              let urlString,
              let url = URL(string: urlString),
              let scheme = url.scheme?.lowercased(),
              scheme == "https" || scheme == "http",
              let host = url.host, !host.isEmpty
        else {
            return nil
        }

        let request = ImageRequest(
            url: url,
            processors: [
                ImageProcessors.Resize(
                    size: CGSize(width: 64, height: 64),
                    unit: .pixels,
                    contentMode: .aspectFit
                )
            ]
        )
        guard let image = try? await pipeline.image(for: request), !Task.isCancelled else {
            return nil
        }
        return image.pngData()
    }
}
