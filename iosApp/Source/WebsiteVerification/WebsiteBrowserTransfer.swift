import FeedFlowKit
import Foundation
import WebKit

@MainActor
struct WebsiteBrowserTransfer {
    let webView: WKWebView
    let approvedOrigin: String

    func capture(urlString: String) async throws -> WebsiteBrowserCapturedResponse {
        let webView = webView
        guard let url = URL(string: urlString), WebsiteProfileIdentity.origin(url) == approvedOrigin,
              WebsiteProfileIdentity.origin(webView.url ?? url) == approvedOrigin
        else {
            throw WebsiteBrowserTransferError(reason: "origin")
        }
        let script = try transferScript()
        let nonce = UUID().uuidString
        let configJSON = try configurationJSON(url: url, nonce: nonce)
        let nonceJSON = try Self.jsonString(nonce)
        try await install(script: script, configuration: configJSON, nonce: nonceJSON, on: webView)
        defer { clearTransfer(nonce: nonceJSON, on: webView) }
        return try await poll(url: url, nonce: nonce, nonceJSON: nonceJSON, on: webView)
    }

    private func transferScript() throws -> String {
        guard let url = Bundle.main.url(forResource: "feed-recovery-transfer", withExtension: "js") else {
            throw WebsiteBrowserTransferError(reason: "owner")
        }
        return try String(contentsOf: url, encoding: .utf8)
    }

    private func configurationJSON(url: URL, nonce: String) throws -> String {
        let config: [String: Any] = [
            "url": url.absoluteString,
            "nonce": nonce,
            "maxBytes": Int(RecoveryPolicy.shared.MAX_BODY_BYTES),
            "chunkBytes": Int(RecoveryPolicy.shared.TRANSFER_CHUNK_BYTES),
            "timeoutMs": Int(RecoveryPolicy.shared.BROWSER_FETCH_TIMEOUT_MILLIS)
        ]
        let json = try JSONSerialization.data(withJSONObject: config, options: [.fragmentsAllowed])
        guard let configJSON = String(data: json, encoding: .utf8) else {
            throw WebsiteBrowserTransferError(reason: "owner")
        }
        return configJSON
    }

    private func install(script: String, configuration: String, nonce: String, on webView: WKWebView) async throws {
        _ = try await webView.evaluateJavaScript(
            """
            \(script)
            globalThis.__feedFlowTransferNonce = \(nonce);
            globalThis.__feedFlowTransfer = __feedFlowCreateTransfer(\(configuration));
            true;
            """
        )
    }

    private func clearTransfer(nonce: String, on webView: WKWebView) {
        webView.evaluateJavaScript(
            """
            if (globalThis.__feedFlowTransferNonce === \(nonce)) {
                globalThis.__feedFlowTransfer?.cancel(\(nonce));
                delete globalThis.__feedFlowTransfer;
                delete globalThis.__feedFlowTransferNonce;
            }
            """
        )
    }

    private func poll(
        url: URL,
        nonce: String,
        nonceJSON: String,
        on webView: WKWebView
    ) async throws -> WebsiteBrowserCapturedResponse {
        var reader = WebsiteBrowserTransferReader(
            url: url,
            nonce: nonce,
            maxBodyBytes: Int(RecoveryPolicy.shared.MAX_BODY_BYTES),
            chunkBytes: Int(RecoveryPolicy.shared.TRANSFER_CHUNK_BYTES)
        )
        let deadline = ContinuousClock.now + .milliseconds(Int(RecoveryPolicy.shared.BROWSER_FETCH_TIMEOUT_MILLIS))
        while ContinuousClock.now < deadline {
            try Task.checkCancellation()
            try validateCurrentOrigin(url: url, on: webView)
            let raw = try await webView.evaluateJavaScript(
                "JSON.stringify(globalThis.__feedFlowTransfer.next(\(nonceJSON)))"
            )
            let message = try reader.message(from: raw)
            switch message["kind"] as? String {
            case "waiting":
                try await Task.sleep(for: .milliseconds(Self.resultPollMillis))
            case "chunk":
                try reader.appendChunk(from: message)
            case "done":
                return try reader.capturedResponse(from: message)
            case "error":
                throw WebsiteBrowserTransferError(reason: message["reason"] as? String ?? "transport")
            default:
                throw WebsiteBrowserTransferError(reason: "owner")
            }
        }
        throw WebsiteBrowserTransferError(reason: "timeout")
    }

    private func validateCurrentOrigin(url: URL, on webView: WKWebView) throws {
        guard WebsiteProfileIdentity.origin(webView.url ?? url) == approvedOrigin else {
            throw WebsiteBrowserTransferError(reason: "navigation")
        }
    }

    private static func jsonString(_ string: String) throws -> String {
        let data = try JSONSerialization.data(withJSONObject: [string])
        guard let array = String(data: data, encoding: .utf8), array.count >= 2 else {
            throw WebsiteBrowserTransferError(reason: "owner")
        }
        return String(array.dropFirst().dropLast())
    }

    private static let resultPollMillis = 250
}
