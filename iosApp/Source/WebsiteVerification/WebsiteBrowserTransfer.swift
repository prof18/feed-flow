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
        var bytes = Data()
        var sequence = 0
        let deadline = ContinuousClock.now + .milliseconds(Int(RecoveryPolicy.shared.BROWSER_FETCH_TIMEOUT_MILLIS))
        while ContinuousClock.now < deadline {
            try Task.checkCancellation()
            try validateCurrentOrigin(url: url, on: webView)
            let message = try await nextMessage(nonceJSON: nonceJSON, nonce: nonce, on: webView)
            switch message["kind"] as? String {
            case "waiting":
                try await Task.sleep(for: .milliseconds(Self.resultPollMillis))
            case "chunk":
                try bytes.append(chunk(from: message, sequence: sequence, byteCount: bytes.count))
                sequence += 1
            case "done":
                return try capturedResponse(from: message, url: url, bytes: bytes)
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

    private func nextMessage(nonceJSON: String, nonce: String, on webView: WKWebView) async throws -> [String: Any] {
        let raw = try await webView.evaluateJavaScript(
            "JSON.stringify(globalThis.__feedFlowTransfer.next(\(nonceJSON)))"
        )
        guard let json = raw as? String,
              let message = try JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String: Any],
              message["nonce"] as? String == nonce,
              message["kind"] is String
        else {
            throw WebsiteBrowserTransferError(reason: "owner")
        }
        return message
    }

    private func chunk(from message: [String: Any], sequence: Int, byteCount: Int) throws -> Data {
        guard let currentSequence = message["sequence"] as? Int,
              currentSequence == sequence,
              let encoded = message["base64"] as? String,
              encoded.utf8.count <= Self.maxEncodedChunk,
              let chunk = Data(base64Encoded: encoded),
              chunk.count <= Int(RecoveryPolicy.shared.TRANSFER_CHUNK_BYTES),
              byteCount + chunk.count <= Int(RecoveryPolicy.shared.MAX_BODY_BYTES)
        else {
            throw WebsiteBrowserTransferError(reason: "too-large")
        }
        return chunk
    }

    private func capturedResponse(
        from message: [String: Any],
        url: URL,
        bytes: Data
    ) throws -> WebsiteBrowserCapturedResponse {
        guard let effectiveURL = message["effectiveUrl"] as? String,
              effectiveURL == url.absoluteString,
              let status = message["status"] as? Int,
              (200 ... 299).contains(status),
              let total = message["total"] as? Int,
              total == bytes.count
        else {
            throw WebsiteBrowserTransferError(reason: "owner")
        }
        return WebsiteBrowserCapturedResponse(
            effectiveURL: effectiveURL,
            statusCode: status,
            contentType: message["contentType"] as? String,
            data: bytes
        )
    }

    private static func jsonString(_ string: String) throws -> String {
        let data = try JSONSerialization.data(withJSONObject: [string])
        guard let array = String(data: data, encoding: .utf8), array.count >= 2 else {
            throw WebsiteBrowserTransferError(reason: "owner")
        }
        return String(array.dropFirst().dropLast())
    }

    private static let resultPollMillis = 250
    private static let maxEncodedChunk = (Int(RecoveryPolicy.shared.TRANSFER_CHUNK_BYTES) + 2) / 3 * 4
}
