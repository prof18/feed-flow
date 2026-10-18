import Foundation
import WebKit

@MainActor
final class FeedRecoveryBrowserProfile {
    private static let fixtureCookieName = "feedflow_recovery_profile_probe"
    private static let fixtureCookieValue = "synthetic-profile-cookie"

    let identifier: UUID
    private let dataStore: WKWebsiteDataStore
    private var webView: WKWebView?

    init(identifier: UUID) throws {
        self.identifier = identifier
        let dataStore = WKWebsiteDataStore(forIdentifier: identifier)
        self.dataStore = dataStore

        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = dataStore
        webView = WKWebView(frame: .zero, configuration: configuration)
    }

    var hasIsolatedWebsiteDataStore: Bool {
        guard let webView else { return false }
        return dataStore.isPersistent
            && dataStore.identifier == identifier
            && webView.configuration.websiteDataStore === dataStore
            && dataStore !== WKWebsiteDataStore.default()
    }

    static func defaultStoreContainsFixtureCookie() async -> Bool {
        let cookies = await allCookies(in: WKWebsiteDataStore.default().httpCookieStore)
        return cookies.contains { $0.name == fixtureCookieName && $0.domain.contains("fixture.test") }
    }

    func setFixtureCookie() async throws {
        guard let cookie = HTTPCookie(properties: [
            .name: Self.fixtureCookieName,
            .value: Self.fixtureCookieValue,
            .domain: "fixture.test",
            .path: "/",
            .secure: "TRUE",
            .expires: Date(timeIntervalSinceNow: 60 * 60),
        ]) else {
            throw FeedRecoveryBrowserProfileError.invalidFixtureCookie
        }
        await withCheckedContinuation { continuation in
            dataStore.httpCookieStore.setCookie(cookie) {
                continuation.resume()
            }
        }
    }

    func containsFixtureCookie() async -> Bool {
        let cookies = await Self.allCookies(in: dataStore.httpCookieStore)
        return cookies.contains {
            $0.name == Self.fixtureCookieName
                && $0.value == Self.fixtureCookieValue
                && $0.domain.contains("fixture.test")
        }
    }

    func clearCookies() async {
        let cookies = await Self.allCookies(in: dataStore.httpCookieStore)
        for cookie in cookies {
            await withCheckedContinuation { continuation in
                dataStore.httpCookieStore.delete(cookie) {
                    continuation.resume()
                }
            }
        }
    }

    func clearAllWebsiteData() async {
        await withCheckedContinuation { continuation in
            dataStore.removeData(
                ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(),
                modifiedSince: .distantPast
            ) {
                continuation.resume()
            }
        }
    }

    func close() {
        webView?.stopLoading()
        webView?.navigationDelegate = nil
        webView?.uiDelegate = nil
        webView = nil
    }

    private static func allCookies(in store: WKHTTPCookieStore) async -> [HTTPCookie] {
        await withCheckedContinuation { continuation in
            store.getAllCookies { cookies in
                continuation.resume(returning: cookies)
            }
        }
    }
}
