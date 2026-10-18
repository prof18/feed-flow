import FeedFlowKit
import Foundation

/// Sends one request with a recovery profile's saved identity. The initializer does no work:
/// the WebKit data store is opened only inside `fetch`.
final class WebsiteSessionBridge: IosWebsiteSessionBridge {
    private let tasks = WebsiteRequestTasks()

    // The exported Kotlin protocol includes its completion callback.
    // swiftlint:disable:next function_parameter_count
    func fetch(
        requestId: String,
        url: String,
        profileId: String,
        userAgent: String,
        accept: String,
        etag: String?,
        lastModified: String?,
        completionHandler: @escaping (IosWebsiteSessionResult?, Error?) -> Void
    ) {
        tasks.start(id: requestId) {
            do {
                guard let endpoint = URL(string: url),
                      let identity = await WebsiteProfileIdentity.identity(
                          profileId: profileId,
                          userAgent: userAgent,
                          url: endpoint
                      )
                else {
                    completionHandler(Self.failure("no-identity"), nil)
                    return
                }
                var request = URLRequest(url: endpoint)
                request.setValue(identity.userAgent, forHTTPHeaderField: "User-Agent")
                request.setValue(accept, forHTTPHeaderField: "Accept")
                request.setValue(identity.cookie, forHTTPHeaderField: "Cookie")
                request.setValue(etag, forHTTPHeaderField: "If-None-Match")
                request.setValue(lastModified, forHTTPHeaderField: "If-Modified-Since")
                let response = try await WebsiteHTTPTransport().fetch(
                    request: request,
                    maxBytes: Int(RecoveryPolicy.shared.MAX_BODY_BYTES)
                )
                completionHandler(Self.result(response), nil)
            } catch is CancellationError {
                completionHandler(nil, CancellationError())
            } catch let WebsiteHTTPTransportError.transport(error) where (error as? URLError)?.code == .timedOut {
                completionHandler(Self.failure("timeout"), nil)
            } catch {
                completionHandler(Self.failure("transport"), nil)
            }
        }
    }

    func cancel(requestId: String) {
        tasks.cancel(id: requestId)
    }

    static func failureReason(_ response: WebsiteHTTPResponse) -> String? {
        if response.challenge { return nil }
        if response.tooLarge { return "too-large" }
        if (300 ... 399).contains(response.statusCode), response.statusCode != 304 { return "redirect" }
        return nil
    }

    private static func result(_ response: WebsiteHTTPResponse) -> IosWebsiteSessionResult {
        let header: (String) -> String? = { name in
            response.headers.first { $0.key.caseInsensitiveCompare(name) == .orderedSame }?.value
        }
        return IosWebsiteSessionResult(
            data: response.data,
            statusCode: Int32(response.statusCode),
            contentType: header("Content-Type"),
            etag: header("ETag"),
            lastModified: header("Last-Modified"),
            cacheControl: header("Cache-Control"),
            expires: header("Expires"),
            date: header("Date"),
            retryAfter: header("Retry-After"),
            challenge: response.challenge,
            failure: failureReason(response)
        )
    }

    private static func failure(_ reason: String) -> IosWebsiteSessionResult {
        IosWebsiteSessionResult(
            data: Data(),
            statusCode: 0,
            contentType: nil,
            etag: nil,
            lastModified: nil,
            cacheControl: nil,
            expires: nil,
            date: nil,
            retryAfter: nil,
            challenge: false,
            failure: reason
        )
    }
}
