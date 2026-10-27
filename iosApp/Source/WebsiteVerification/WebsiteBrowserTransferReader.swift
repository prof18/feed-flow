import Foundation

/// Validates the messages the in-page transfer script returns and reassembles the body.
struct WebsiteBrowserTransferReader {
    let url: URL
    let nonce: String
    let maxBodyBytes: Int
    let chunkBytes: Int
    private(set) var bytes = Data()
    private var sequence = 0

    init(url: URL, nonce: String, maxBodyBytes: Int, chunkBytes: Int) {
        self.url = url
        self.nonce = nonce
        self.maxBodyBytes = maxBodyBytes
        self.chunkBytes = chunkBytes
    }

    func message(from raw: Any?) throws -> [String: Any] {
        guard let json = raw as? String,
              let message = try JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String: Any],
              message["nonce"] as? String == nonce,
              message["kind"] is String
        else {
            throw WebsiteBrowserTransferError(reason: "owner")
        }
        return message
    }

    mutating func appendChunk(from message: [String: Any]) throws {
        guard let currentSequence = message["sequence"] as? Int,
              currentSequence == sequence,
              let encoded = message["base64"] as? String,
              encoded.utf8.count <= maxEncodedChunk,
              let chunk = Data(base64Encoded: encoded),
              chunk.count <= chunkBytes,
              bytes.count + chunk.count <= maxBodyBytes
        else {
            throw WebsiteBrowserTransferError(reason: "too-large")
        }
        bytes.append(chunk)
        sequence += 1
    }

    func capturedResponse(from message: [String: Any]) throws -> WebsiteBrowserCapturedResponse {
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

    private var maxEncodedChunk: Int {
        (chunkBytes + 2) / 3 * 4
    }
}
