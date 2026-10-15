import FeedFlowKit
import Nuke
import UIKit
import XCTest

final class NotificationImageAttachmentLoaderTests: XCTestCase {
    func testThumbnailRequestAndEncodedImageBounds() async throws {
        let bytes = try imageData(width: 4096, height: 2048)
        let pipeline = pipeline(data: bytes)
        let directory = uniqueDirectory()
        let loader = NotificationImageAttachmentLoader(pipeline: pipeline, directory: directory)
        let fixtureURL = try XCTUnwrap(URL(string: "https://fixture.test/large"))
        let request = NotificationImageAttachmentLoader.request(for: fixtureURL)
        XCTAssertEqual(request.thumbnail, ImageRequest.ThumbnailOptions(maxPixelSize: 1024))
        let loaded = try await loader.load(urlString: "https://fixture.test/large", allowanceMillis: 1000)
        let url = try XCTUnwrap(loaded)
        defer { try? FileManager.default.removeItem(at: directory) }
        let staged = try Data(contentsOf: url)
        let decoded = try XCTUnwrap(UIImage(data: staged)?.cgImage)
        XCTAssertLessThanOrEqual(decoded.width, 1024)
        XCTAssertLessThanOrEqual(decoded.height, 1024)
        XCTAssertEqual(decoded.width, decoded.height * 2)
        XCTAssertLessThanOrEqual(staged.count, 2_097_152)
    }

    func testSmallImagesAreNotUpscaledAndInvalidInputsFallBack() async throws {
        let directory = uniqueDirectory()
        defer { try? FileManager.default.removeItem(at: directory) }
        let loader = NotificationImageAttachmentLoader(pipeline: pipeline(data: try imageData(width: 16, height: 8)), directory: directory)
        for invalid in [nil, "", "relative.png", "file:///tmp/a", "https:///missing"] {
            let result = try await loader.load(urlString: invalid, allowanceMillis: 1000)
            XCTAssertNil(result)
        }
        let loaded = try await loader.load(urlString: "https://fixture.test/small", allowanceMillis: 1000)
        let url = try XCTUnwrap(loaded)
        let image = try XCTUnwrap(UIImage(contentsOfFile: url.path)?.cgImage)
        XCTAssertEqual(image.width, 16)
        XCTAssertEqual(image.height, 8)
        let invalid = NotificationImageAttachmentLoader(pipeline: pipeline(data: Data("broken".utf8)), directory: directory)
        let badResult = try await invalid.load(urlString: "https://fixture.test/broken", allowanceMillis: 1000)
        XCTAssertNil(badResult)
    }

    func testTimeoutAndParentCancellationCancelNukeRequest() async throws {
        let directory = uniqueDirectory()
        defer { try? FileManager.default.removeItem(at: directory) }
        let stopped = expectation(description: "timeout stops URLProtocol")
        NotificationImageURLProtocol.stopped = { stopped.fulfill() }
        let loader = NotificationImageAttachmentLoader(pipeline: pipeline(data: nil), directory: directory)
        let timedOut = try await loader.load(urlString: "https://fixture.test/timeout", allowanceMillis: 20)
        XCTAssertNil(timedOut)
        await fulfillment(of: [stopped], timeout: 1)

        let began = expectation(description: "request started")
        let cancelled = expectation(description: "parent cancellation stops URLProtocol")
        NotificationImageURLProtocol.started = { began.fulfill() }
        NotificationImageURLProtocol.stopped = { cancelled.fulfill() }
        let task = Task { try await loader.load(urlString: "https://fixture.test/cancel", allowanceMillis: 1000) }
        await fulfillment(of: [began], timeout: 1)
        task.cancel()
        do { _ = try await task.value; XCTFail("must cancel") } catch { XCTAssertTrue(error is CancellationError) }
        await fulfillment(of: [cancelled], timeout: 1)
        XCTAssertTrue((try? FileManager.default.contentsOfDirectory(atPath: directory.path).isEmpty) ?? true)
        NotificationImageURLProtocol.started = nil
        NotificationImageURLProtocol.stopped = nil
    }

    func testStartupRemovesOnlyOldFilesInItsDirectory() throws {
        let directory = uniqueDirectory()
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let old = directory.appendingPathComponent("old.jpg")
        let recent = directory.appendingPathComponent("recent.jpg")
        try Data().write(to: old)
        try Data().write(to: recent)
        try FileManager.default.setAttributes([.modificationDate: Date(timeIntervalSinceNow: -90_000)], ofItemAtPath: old.path)
        _ = NotificationImageAttachmentLoader(pipeline: pipeline(data: nil), directory: directory)
        XCTAssertFalse(FileManager.default.fileExists(atPath: old.path))
        XCTAssertTrue(FileManager.default.fileExists(atPath: recent.path))
    }

    private func uniqueDirectory() -> URL {
        FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    }

    private func imageData(width: Int, height: Int) throws -> Data {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: width, height: height), format: format)
        let image = renderer.image { context in
            UIColor.blue.setFill()
            context.fill(CGRect(x: 0, y: 0, width: width, height: height))
        }
        return try XCTUnwrap(image.jpegData(compressionQuality: 0.8))
    }

    private func pipeline(data: Data?) -> ImagePipeline {
        NotificationImageURLProtocol.bytes = data
        let session = URLSessionConfiguration.ephemeral
        session.protocolClasses = [NotificationImageURLProtocol.self]
        var configuration = ImagePipeline.Configuration()
        configuration.dataLoader = DataLoader(configuration: session)
        configuration.imageCache = nil
        configuration.dataCache = nil
        return ImagePipeline(configuration: configuration)
    }
}
