import Foundation
import XCTest

final class WebsiteHTTPTransportTests: XCTestCase {
    override func setUp() {
        super.setUp()
        WebsiteHTTPURLProtocol.reset()
    }

    override func tearDown() {
        WebsiteHTTPURLProtocol.reset()
        super.tearDown()
    }

    func testChallengeHeaderReturnsBeforeReadingBodyForAnyStatus() async throws {
        for status in [200, 403] {
            let url = try XCTUnwrap(URL(string: "https://fixture.test/challenge-\(status)"))
            WebsiteHTTPURLProtocol.routes[url] = .headersOnly(
                status: status,
                headers: ["cf-mitigated": " challenge "]
            )

            let response = try await transport().fetch(
                request: URLRequest(url: url), maxBytes: 4
            )

            XCTAssertEqual(response.statusCode, status)
            XCTAssertTrue(response.challenge, "Expected challenge marker for fixture status \(status)")
            XCTAssertFalse(response.tooLarge)
            XCTAssertTrue(response.data.isEmpty)
            XCTAssertEqual(WebsiteHTTPURLProtocol.deliveredBodyBytes, 0)
        }
    }

    func testOrdinaryDenialReturnsHeadersWithoutReadingUnsupportedBody() async throws {
        let url = try XCTUnwrap(URL(string: "https://fixture.test/denied"))
        WebsiteHTTPURLProtocol.routes[url] = .headersOnly(
            status: 403,
            headers: ["Retry-After": "30"]
        )

        let response = try await transport().fetch(request: URLRequest(url: url), maxBytes: 100)

        XCTAssertEqual(response.statusCode, 403)
        XCTAssertEqual(response.headers["Retry-After"], "30")
        XCTAssertFalse(response.challenge)
        XCTAssertTrue(response.data.isEmpty)
        XCTAssertEqual(WebsiteHTTPURLProtocol.deliveredBodyBytes, 0)
    }

    func testSuccessfulResponseAndStreamedByteCap() async throws {
        let url = try XCTUnwrap(URL(string: "https://fixture.test/feed"))
        WebsiteHTTPURLProtocol.routes[url] = .response(
            status: 200,
            headers: ["Content-Type": "application/rss+xml", "Content-Length": "6"],
            body: Data("123456".utf8)
        )

        let response = try await transport().fetch(request: URLRequest(url: url), maxBytes: 8)
        XCTAssertEqual(response.data, Data("123456".utf8))
        XCTAssertFalse(response.tooLarge)

        WebsiteHTTPURLProtocol.routes[url] = .response(status: 200, headers: [:], body: Data("123456".utf8))
        let oversized = try await transport().fetch(request: URLRequest(url: url), maxBytes: 5)
        XCTAssertTrue(oversized.tooLarge)
        XCTAssertTrue(oversized.data.isEmpty)
    }

    func testDeclaredOversizeIsRejectedBeforeBody() async throws {
        let url = try XCTUnwrap(URL(string: "https://fixture.test/large"))
        WebsiteHTTPURLProtocol.routes[url] = .headersOnly(status: 200, headers: ["Content-Length": "6"])

        let response = try await transport().fetch(request: URLRequest(url: url), maxBytes: 5)

        XCTAssertTrue(response.tooLarge)
        XCTAssertEqual(WebsiteHTTPURLProtocol.deliveredBodyBytes, 0)
    }

    func testBlockedRedirectReturnsRedirectWithoutDispatchingDestinationOrForwardingCookies() async throws {
        let start = try XCTUnwrap(URL(string: "https://fixture.test/start"))
        let destination = try XCTUnwrap(URL(string: "https://other.test/final"))
        WebsiteHTTPURLProtocol.routes[start] = .redirect(to: destination)
        var request = URLRequest(url: start)
        request.setValue("sensitive=value", forHTTPHeaderField: "Cookie")

        let response = try await transport().fetch(request: request, maxBytes: 100)

        XCTAssertEqual(response.statusCode, 302)
        XCTAssertEqual(WebsiteHTTPURLProtocol.requests.map(\.url), [start])
        XCTAssertEqual(WebsiteHTTPURLProtocol.requests.first?.value(forHTTPHeaderField: "Cookie"), "sensitive=value")
    }

    func testChallengeRedirectReturnsMarkedResponseWithoutDispatchingDestination() async throws {
        let start = try XCTUnwrap(URL(string: "https://fixture.test/challenge-redirect"))
        let destination = try XCTUnwrap(URL(string: "https://other.test/final"))
        WebsiteHTTPURLProtocol.routes[start] = .redirect(
            to: destination,
            headers: ["cf-mitigated": "challenge", "Retry-After": "30"]
        )
        WebsiteHTTPURLProtocol.routes[destination] = .response(status: 200, headers: [:], body: Data("ok".utf8))

        let response = try await transport().fetch(request: URLRequest(url: start), maxBytes: 100)

        XCTAssertEqual(response.effectiveURL, start)
        XCTAssertEqual(response.statusCode, 302)
        XCTAssertEqual(response.headers["Retry-After"], "30")
        XCTAssertTrue(response.challenge)
        XCTAssertTrue(response.data.isEmpty)
        XCTAssertEqual(WebsiteHTTPURLProtocol.requests.map(\.url), [start])
    }

    func testInvalidRedirectTargetIsNotFollowed() async throws {
        let start = try XCTUnwrap(URL(string: "https://fixture.test/start"))
        let destination = try XCTUnwrap(URL(string: "http://user:pass@other.test/final"))
        WebsiteHTTPURLProtocol.routes[start] = .redirect(to: destination)

        let response = try await transport().fetch(request: URLRequest(url: start), maxBytes: 100)

        XCTAssertEqual(response.statusCode, 302)
        XCTAssertEqual(WebsiteHTTPURLProtocol.requests.map(\.url), [start])
    }

    func testSessionRequestNeverFollowsRedirect() async throws {
        let start = try XCTUnwrap(URL(string: "https://fixture.test/start"))
        let destination = try XCTUnwrap(URL(string: "https://fixture.test/final"))
        WebsiteHTTPURLProtocol.routes[start] = .redirect(to: destination)
        WebsiteHTTPURLProtocol.routes[destination] = .response(status: 200, headers: [:], body: Data("ok".utf8))

        let response = try await transport().fetch(request: URLRequest(url: start), maxBytes: 100)

        XCTAssertEqual(response.statusCode, 302)
        XCTAssertEqual(WebsiteSessionBridge.failureReason(response), "redirect")
        XCTAssertEqual(WebsiteHTTPURLProtocol.requests.map(\.url), [start])
    }

    func testCancellationCompletesOwnedOperation() async throws {
        let url = try XCTUnwrap(URL(string: "https://fixture.test/slow"))
        WebsiteHTTPURLProtocol.routes[url] = .wait
        let started = WebsiteHTTPURLProtocol.makeRequestStartedExpectation()
        let task = Task {
            try await transport().fetch(request: URLRequest(url: url), maxBytes: 100)
        }
        await fulfillment(of: [started], timeout: 2)
        task.cancel()

        do {
            _ = try await task.value
            XCTFail("Cancelled fetch should throw")
        } catch {
            XCTAssertTrue(error is CancellationError || (error as? URLError)?.code == .cancelled)
        }
    }

    private func transport() -> WebsiteHTTPTransport {
        WebsiteHTTPTransport(protocolClasses: [WebsiteHTTPURLProtocol.self])
    }
}
