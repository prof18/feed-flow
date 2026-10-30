import FeedFlowKit
import Foundation
import XCTest

final class FeedConditionalGetURLProtocolTests: XCTestCase {
    private let feedURL = "https://feeds.fixture.test/rss.xml"
    private var store: FeedHttpCacheStore!
    private var originalCacheStore: (() -> FeedHttpCacheStore)!
    private var originalSession: URLSession!
    private var client: URLSession!

    override func setUp() {
        super.setUp()
        FeedOriginStubURLProtocol.reset()
        let store = FeedHttpCacheStore(
            currentTimeMillis: { KotlinLong(value: 0) },
            logger: KermitLogger.companion.withTag(tag: "FeedConditionalGetTests")
        )
        self.store = store
        originalCacheStore = FeedConditionalGetURLProtocol.cacheStore
        originalSession = FeedConditionalGetURLProtocol.session
        FeedConditionalGetURLProtocol.cacheStore = { store }
        FeedConditionalGetURLProtocol.session = session(protocol: FeedOriginStubURLProtocol.self)
        client = session(protocol: FeedConditionalGetURLProtocol.self)
    }

    override func tearDown() {
        FeedConditionalGetURLProtocol.cacheStore = originalCacheStore
        FeedConditionalGetURLProtocol.session = originalSession
        FeedOriginStubURLProtocol.reset()
        super.tearDown()
    }

    func testHandlesOnlyWebRequestsThatWereNotHandledYet() throws {
        XCTAssertTrue(try FeedConditionalGetURLProtocol.canInit(with: request("https://fixture.test/feed")))
        XCTAssertTrue(try FeedConditionalGetURLProtocol.canInit(with: request("HTTP://fixture.test/feed")))
        XCTAssertFalse(try FeedConditionalGetURLProtocol.canInit(with: request("ftp://fixture.test/feed")))
        XCTAssertFalse(try FeedConditionalGetURLProtocol.canInit(with: request("file:///tmp/feed.xml")))

        let handled = try XCTUnwrap(
            (request("https://fixture.test/feed") as NSURLRequest).mutableCopy() as? NSMutableURLRequest
        )
        URLProtocol.setProperty(true, forKey: "FeedConditionalGetHandled", in: handled)
        XCTAssertFalse(FeedConditionalGetURLProtocol.canInit(with: handled as URLRequest))
    }

    func testStoredValidatorsAreSentAsConditionalHeaders() async throws {
        store.seedValidators(urlToValidators: [
            feedURL: FeedHttpValidators(etag: "\"v1\"", lastModified: "Wed, 21 Oct 2026 07:28:00 GMT")
        ])
        FeedOriginStubURLProtocol.response = FeedOriginStubResponse(status: 304, headers: [:], body: Data())

        let (_, response) = try await client.data(for: request(feedURL))

        let sent = try XCTUnwrap(FeedOriginStubURLProtocol.requests.first)
        XCTAssertEqual(sent.value(forHTTPHeaderField: "If-None-Match"), "\"v1\"")
        XCTAssertEqual(sent.value(forHTTPHeaderField: "If-Modified-Since"), "Wed, 21 Oct 2026 07:28:00 GMT")
        XCTAssertEqual((response as? HTTPURLResponse)?.statusCode, 304)
        XCTAssertEqual(store.responseFor(url: feedURL)?.statusCode, 304)
    }

    func testNoConditionalHeadersWithoutStoredValidators() async throws {
        _ = try await client.data(for: request(feedURL))

        let sent = try XCTUnwrap(FeedOriginStubURLProtocol.requests.first)
        XCTAssertNil(sent.value(forHTTPHeaderField: "If-None-Match"))
        XCTAssertNil(sent.value(forHTTPHeaderField: "If-Modified-Since"))
    }

    func testCachingHeadersAreRecordedAndBodyIsDelivered() async throws {
        FeedOriginStubURLProtocol.response = FeedOriginStubResponse(
            status: 200,
            headers: [
                "ETag": "\"v2\"",
                "Last-Modified": "Thu, 22 Oct 2026 07:28:00 GMT",
                "Cache-Control": "max-age=600",
                "Expires": "Thu, 22 Oct 2026 08:28:00 GMT",
                "Date": "Thu, 22 Oct 2026 07:30:00 GMT",
                "Retry-After": "120"
            ],
            body: Data("<rss/>".utf8)
        )

        let (data, _) = try await client.data(for: request(feedURL))

        XCTAssertEqual(String(data: data, encoding: .utf8), "<rss/>")
        let recorded = try XCTUnwrap(store.responseFor(url: feedURL))
        XCTAssertEqual(recorded.statusCode, 200)
        XCTAssertEqual(recorded.etag, "\"v2\"")
        XCTAssertEqual(recorded.lastModified, "Thu, 22 Oct 2026 07:28:00 GMT")
        XCTAssertEqual(recorded.cacheControl, "max-age=600")
        XCTAssertEqual(recorded.expires, "Thu, 22 Oct 2026 08:28:00 GMT")
        XCTAssertEqual(recorded.date, "Thu, 22 Oct 2026 07:30:00 GMT")
        XCTAssertEqual(recorded.retryAfter, "120")
        XCTAssertEqual(store.storedValidatorsFor(url: feedURL)?.etag, "\"v2\"")
        XCTAssertNil(store.challengeFor(url: feedURL))
    }

    func testCloudflareChallengeIsMarked() async throws {
        FeedOriginStubURLProtocol.response = FeedOriginStubResponse(
            status: 403,
            headers: ["cf-mitigated": " challenge "],
            body: Data()
        )

        _ = try await client.data(for: request(feedURL))

        let challenge = try XCTUnwrap(store.challengeFor(url: feedURL))
        XCTAssertEqual(challenge.requestedUrl, feedURL)
        XCTAssertEqual(challenge.effectiveUrl, feedURL)
        XCTAssertEqual(challenge.statusCode, 403)
    }

    func testOtherMitigationsAreNotChallenges() async throws {
        FeedOriginStubURLProtocol.response = FeedOriginStubResponse(
            status: 403,
            headers: ["cf-mitigated": "block"],
            body: Data()
        )

        _ = try await client.data(for: request(feedURL))

        XCTAssertNil(store.challengeFor(url: feedURL))
        XCTAssertEqual(store.responseFor(url: feedURL)?.statusCode, 403)
    }

    private func request(_ value: String) throws -> URLRequest {
        try URLRequest(url: XCTUnwrap(URL(string: value)))
    }

    private func session(protocol protocolClass: AnyClass) -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [protocolClass]
        configuration.urlCache = nil
        return URLSession(configuration: configuration)
    }
}
