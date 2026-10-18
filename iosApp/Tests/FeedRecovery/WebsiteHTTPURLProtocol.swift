import Foundation
import XCTest

final class WebsiteHTTPURLProtocol: URLProtocol {
    private static let lock = NSLock()
    private static var routeTable: [URL: WebsiteHTTPURLProtocolRoute] = [:]
    private static var capturedRequests: [URLRequest] = []
    private static var bodyBytes = 0
    private static var startedExpectation: XCTestExpectation?

    static var routes: [URL: WebsiteHTTPURLProtocolRoute] {
        get { lock.lock(); defer { lock.unlock() }; return routeTable }
        set { lock.lock(); defer { lock.unlock() }; routeTable = newValue }
    }

    static var requests: [URLRequest] {
        lock.lock(); defer { lock.unlock() }; return capturedRequests
    }

    static var deliveredBodyBytes: Int {
        lock.lock(); defer { lock.unlock() }; return bodyBytes
    }

    static func makeRequestStartedExpectation() -> XCTestExpectation {
        lock.lock()
        defer { lock.unlock() }
        let expectation = XCTestExpectation(description: "URLProtocol request started")
        startedExpectation = expectation
        return expectation
    }

    static func reset() {
        lock.lock()
        routeTable = [:]
        capturedRequests = []
        bodyBytes = 0
        startedExpectation = nil
        lock.unlock()
    }

    override class func canInit(with request: URLRequest) -> Bool {
        request.url != nil
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let url = request.url else { return }
        Self.lock.lock()
        Self.capturedRequests.append(request)
        let route = Self.routeTable[url]
        let expectation = Self.startedExpectation
        Self.startedExpectation = nil
        Self.lock.unlock()
        expectation?.fulfill()

        guard let route else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }
        switch route {
        case let .response(status, headers, body):
            send(status: status, headers: headers, body: body)
        case let .headersOnly(status, headers):
            guard let response = httpResponse(status: status, headers: headers) else { return }
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocolDidFinishLoading(self)
        case let .redirect(destination, headers):
            var redirectHeaders = headers
            redirectHeaders["Location"] = destination.absoluteString
            redirectHeaders["Content-Length"] = "0"
            guard let response = httpResponse(
                status: 302,
                headers: redirectHeaders
            ) else { return }
            var redirected = URLRequest(url: destination)
            redirected.httpMethod = request.httpMethod
            client?.urlProtocol(self, wasRedirectedTo: redirected, redirectResponse: response)
        case .wait:
            break
        }
    }

    override func stopLoading() {}

    private func send(status: Int, headers: [String: String], body: Data) {
        guard let response = httpResponse(status: status, headers: headers) else { return }
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        if !body.isEmpty {
            Self.lock.lock()
            Self.bodyBytes += body.count
            Self.lock.unlock()
            client?.urlProtocol(self, didLoad: body)
        }
        client?.urlProtocolDidFinishLoading(self)
    }

    private func httpResponse(status: Int, headers: [String: String]) -> HTTPURLResponse? {
        guard let url = request.url else { return nil }
        return HTTPURLResponse(url: url, statusCode: status, httpVersion: "HTTP/1.1", headerFields: headers)
    }
}
