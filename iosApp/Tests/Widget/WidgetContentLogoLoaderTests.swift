import Nuke
import UIKit
import XCTest

final class WidgetContentLogoLoaderTests: XCTestCase {
    func testMissingAndNonNetworkURLsDoNotMakeRequests() async {
        let loader = WidgetLogoDataLoader(data: nil)
        let pipeline = makePipeline(loader: loader)
        for url in [nil, "", "file:///tmp/logo.png", "data:image/png;base64,aA==", "https://"] {
            let data = await WidgetContentLogoLoader.imageData(for: url, pipeline: pipeline)
            XCTAssertNil(data)
        }
        XCTAssertEqual(loader.requestCount, 0)
    }

    func testFailedRequestAndInvalidImageReturnFallback() async {
        for bytes in [nil, Data("not an image".utf8)] {
            let loader = WidgetLogoDataLoader(data: bytes)
            let data = await WidgetContentLogoLoader.imageData(
                for: "https://example.com/logo.png",
                pipeline: makePipeline(loader: loader)
            )
            XCTAssertNil(data)
            XCTAssertEqual(loader.requestCount, 1)
        }
    }

    func testLogoIsResizedAndCachedWithoutAnotherRequest() async throws {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 256, height: 128), format: format)
        let bytes = renderer.pngData { context in
            UIColor.red.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 256, height: 128))
        }
        let loader = WidgetLogoDataLoader(data: bytes)
        let pipeline = makePipeline(loader: loader)
        let first = await WidgetContentLogoLoader.imageData(
            for: "https://example.com/logo.png", pipeline: pipeline
        )
        let data = try XCTUnwrap(first)
        let image = try XCTUnwrap(UIImage(data: data))
        XCTAssertEqual(image.size.width, 64)
        XCTAssertEqual(image.size.height, 32)

        let second = await WidgetContentLogoLoader.imageData(
            for: "https://example.com/logo.png", pipeline: pipeline
        )
        XCTAssertEqual(second, data)
        XCTAssertEqual(loader.requestCount, 1)
    }

    func testGenericFeedSymbolExists() {
        XCTAssertNotNil(UIImage(systemName: WidgetContentLogoLoader.fallbackSymbol))
    }

    private func makePipeline(loader: WidgetLogoDataLoader) -> ImagePipeline {
        var configuration = ImagePipeline.Configuration()
        configuration.dataLoader = loader
        configuration.imageCache = ImageCache()
        configuration.isRateLimiterEnabled = false
        return ImagePipeline(configuration: configuration)
    }
}
