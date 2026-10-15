import FeedFlowKit
import Foundation
import Nuke
import UIKit

final class NotificationImageAttachmentLoader {
    private let pipeline: ImagePipeline
    private let directory: URL

    init(
        pipeline: ImagePipeline = NotificationImageAttachmentLoader.pipeline,
        directory: URL = FileManager.default.temporaryDirectory.appendingPathComponent(
            ArticleNotificationPolicy.shared.ATTACHMENT_DIRECTORY, isDirectory: true
        )
    ) {
        self.pipeline = pipeline
        self.directory = directory
        removeOrphans()
    }

    func load(urlString: String?, allowanceMillis: Int64) async throws -> URL? {
        try Task.checkCancellation()
        guard allowanceMillis > 0, let url = webURL(urlString) else { return nil }

        return try await withThrowingTaskGroup(of: Result<URL?, Error>.self) { group in
            group.addTask {
                do { return .success(try await self.stage(url: url)) } catch { return .failure(error) }
            }
            group.addTask {
                do {
                    try await Task.sleep(for: .milliseconds(allowanceMillis))
                    return .success(nil)
                } catch { return .failure(error) }
            }
            guard let first = try await group.next() else { return nil }
            group.cancelAll()
            // A decoder/file write can finish just as the timer wins. Await and clean
            // its result too, so a timed-out image cannot leave a staged file behind.
            for try await result in group {
                if case let .success(url?) = result { try? FileManager.default.removeItem(at: url) }
            }
            let staged: URL?
            switch first {
            case let .success(url): staged = url
            case let .failure(error):
                try Task.checkCancellation()
                if error is CancellationError { throw error }
                return nil
            }
            do {
                try Task.checkCancellation()
                return staged
            } catch {
                removeStaged(staged)
                throw error
            }
        }
    }

    private func removeStaged(_ url: URL?) {
        guard let url else { return }
        try? FileManager.default.removeItem(at: url)
    }

    private func webURL(_ value: String?) -> URL? {
        guard let value, let url = URL(string: value),
              let scheme = url.scheme?.lowercased(), ["http", "https"].contains(scheme),
              let host = url.host, !host.isEmpty else { return nil }
        return url
    }

    static func request(for url: URL) -> ImageRequest {
        var request = ImageRequest(url: url)
        request.thumbnail = ImageRequest.ThumbnailOptions(
            maxPixelSize: Float(ArticleNotificationPolicy.shared.IMAGE_EDGE_PX)
        )
        return request
    }

    private func stage(url: URL) async throws -> URL? {
        let image = try await pipeline.image(for: Self.request(for: url))
        try Task.checkCancellation()
        guard let data = image.jpegData(compressionQuality: 0.8),
              data.count <= Int(ArticleNotificationPolicy.shared.ATTACHMENT_MAX_BYTES) else { return nil }
        try Task.checkCancellation()
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let staged = directory.appendingPathComponent(UUID().uuidString + ".jpg")
        do {
            try data.write(to: staged, options: .atomic)
            try Task.checkCancellation()
            return staged
        } catch {
            try? FileManager.default.removeItem(at: staged)
            throw error
        }
    }

    private func removeOrphans() {
        let keys: Set<URLResourceKey> = [.contentModificationDateKey, .isRegularFileKey]
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: directory, includingPropertiesForKeys: Array(keys)
        ) else { return }
        let cutoff = Date(timeIntervalSinceNow: -24 * 60 * 60)
        for file in files {
            guard let values = try? file.resourceValues(forKeys: keys), values.isRegularFile == true,
                  let modified = values.contentModificationDate, modified < cutoff else { continue }
            try? FileManager.default.removeItem(at: file)
        }
    }

    private static let pipeline: ImagePipeline = {
        var configuration = ImagePipeline.Configuration()
        let session = URLSessionConfiguration.ephemeral
        session.urlCache = nil
        let timeout = Double(ArticleNotificationPolicy.shared.IMAGE_TIMEOUT_MS) / 1_000
        session.timeoutIntervalForRequest = timeout
        session.timeoutIntervalForResource = timeout
        configuration.dataLoader = DataLoader(configuration: session)
        return ImagePipeline(configuration: configuration)
    }()
}
