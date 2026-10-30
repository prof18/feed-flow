import Foundation

/// Stands in for the feed server behind `FeedConditionalGetURLProtocol`'s own session.
final class FeedOriginStubURLProtocol: URLProtocol {
    private static let lock = NSLock()
    private static var stubbedResponse = FeedOriginStubResponse(status: 200, headers: [:], body: Data())
    private static var capturedRequests: [URLRequest] = []

    static var response: FeedOriginStubResponse {
        get { lock.lock(); defer { lock.unlock() }; return stubbedResponse }
        set { lock.lock(); defer { lock.unlock() }; stubbedResponse = newValue }
    }

    static var requests: [URLRequest] {
        lock.lock(); defer { lock.unlock() }; return capturedRequests
    }

    static func reset() {
        lock.lock()
        stubbedResponse = FeedOriginStubResponse(status: 200, headers: [:], body: Data())
        capturedRequests = []
        lock.unlock()
    }

    override class func canInit(with _: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let url = request.url else { return }
        Self.lock.lock()
        Self.capturedRequests.append(request)
        let stub = Self.stubbedResponse
        Self.lock.unlock()

        guard let response = HTTPURLResponse(
            url: url,
            statusCode: stub.status,
            httpVersion: "HTTP/1.1",
            headerFields: stub.headers
        ) else { return }
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        if !stub.body.isEmpty {
            client?.urlProtocol(self, didLoad: stub.body)
        }
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

struct FeedOriginStubResponse {
    let status: Int
    let headers: [String: String]
    let body: Data
}
