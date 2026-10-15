import Foundation

final class NotificationImageURLProtocol: URLProtocol {
    static var bytes: Data?
    static var started: (() -> Void)?
    static var stopped: (() -> Void)?

    override class func canInit(with _: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        Self.started?()
        guard let bytes = Self.bytes, let url = request.url else { return }
        let response = HTTPURLResponse(url: url, statusCode: 200, httpVersion: nil, headerFields: ["Content-Type": "image/jpeg"])
        if let response { client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed) }
        client?.urlProtocol(self, didLoad: bytes)
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() { Self.stopped?() }
}
