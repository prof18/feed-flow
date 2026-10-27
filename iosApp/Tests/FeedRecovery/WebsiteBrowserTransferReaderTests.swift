import Foundation
import XCTest

final class WebsiteBrowserTransferReaderTests: XCTestCase {
    private let urlString = "https://fixture.test/feed.xml"
    private let nonce = "nonce-1"

    func testChunksAreReassembledIntoTheCapturedResponse() throws {
        var reader = try makeReader()
        try reader.appendChunk(from: chunk(sequence: 0, Data("<rss>".utf8)))
        try reader.appendChunk(from: chunk(sequence: 1, Data("</rss>".utf8)))

        let response = try reader.capturedResponse(from: done(total: 11, contentType: "application/rss+xml"))

        XCTAssertEqual(String(data: response.data, encoding: .utf8), "<rss></rss>")
        XCTAssertEqual(response.effectiveURL, urlString)
        XCTAssertEqual(response.statusCode, 200)
        XCTAssertEqual(response.contentType, "application/rss+xml")
    }

    func testMessageForAnotherNonceIsRejected() throws {
        let reader = try makeReader()
        assertReason("owner") {
            _ = try reader.message(from: #"{"nonce":"other","kind":"waiting"}"#)
        }
    }

    func testMessageWithoutKindOrNotAStringIsRejected() throws {
        let reader = try makeReader()
        assertReason("owner") { _ = try reader.message(from: #"{"nonce":"nonce-1"}"#) }
        assertReason("owner") { _ = try reader.message(from: 42) }
        assertReason("owner") { _ = try reader.message(from: nil) }
    }

    func testMessageWithMatchingNonceIsReturned() throws {
        let message = try makeReader().message(from: #"{"nonce":"nonce-1","kind":"waiting"}"#)
        XCTAssertEqual(message["kind"] as? String, "waiting")
    }

    func testOutOfOrderChunkIsRejected() throws {
        var reader = try makeReader()
        assertReason("too-large") { try reader.appendChunk(from: self.chunk(sequence: 1, Data("a".utf8))) }
    }

    func testReplayedChunkIsRejected() throws {
        var reader = try makeReader()
        try reader.appendChunk(from: chunk(sequence: 0, Data("a".utf8)))
        assertReason("too-large") { try reader.appendChunk(from: self.chunk(sequence: 0, Data("a".utf8))) }
    }

    func testChunkLargerThanTheChunkLimitIsRejected() throws {
        var reader = try makeReader(chunkBytes: 4)
        assertReason("too-large") { try reader.appendChunk(from: self.chunk(sequence: 0, Data("12345".utf8))) }
    }

    func testBodyLargerThanTheBodyLimitIsRejected() throws {
        var reader = try makeReader(maxBodyBytes: 6, chunkBytes: 4)
        try reader.appendChunk(from: chunk(sequence: 0, Data("1234".utf8)))
        assertReason("too-large") { try reader.appendChunk(from: self.chunk(sequence: 1, Data("567".utf8))) }
        XCTAssertEqual(reader.bytes.count, 4)
    }

    func testInvalidBase64ChunkIsRejected() throws {
        var reader = try makeReader()
        assertReason("too-large") {
            try reader.appendChunk(from: ["sequence": 0, "base64": "not base64!"])
        }
    }

    func testDoneForAnotherURLIsRejected() throws {
        let reader = try makeReader()
        var message = done(total: 0)
        message["effectiveUrl"] = "https://fixture.test/redirected.xml"
        assertReason("owner") { _ = try reader.capturedResponse(from: message) }
    }

    func testDoneWithNonSuccessStatusIsRejected() throws {
        let reader = try makeReader()
        for status in [199, 304, 403, 500] {
            var message = done(total: 0)
            message["status"] = status
            assertReason("owner") { _ = try reader.capturedResponse(from: message) }
        }
    }

    func testDoneWithMismatchedTotalIsRejected() throws {
        var reader = try makeReader()
        try reader.appendChunk(from: chunk(sequence: 0, Data("abc".utf8)))
        assertReason("owner") { _ = try reader.capturedResponse(from: self.done(total: 4)) }
    }

    private func makeReader(maxBodyBytes: Int = 1024, chunkBytes: Int = 64) throws -> WebsiteBrowserTransferReader {
        try WebsiteBrowserTransferReader(
            url: XCTUnwrap(URL(string: urlString)),
            nonce: nonce,
            maxBodyBytes: maxBodyBytes,
            chunkBytes: chunkBytes
        )
    }

    private func chunk(sequence: Int, _ data: Data) -> [String: Any] {
        ["nonce": nonce, "kind": "chunk", "sequence": sequence, "base64": data.base64EncodedString()]
    }

    private func done(total: Int, contentType: String? = nil) -> [String: Any] {
        var message: [String: Any] = [
            "nonce": nonce,
            "kind": "done",
            "effectiveUrl": urlString,
            "status": 200,
            "total": total
        ]
        message["contentType"] = contentType
        return message
    }

    private func assertReason(
        _ reason: String,
        file: StaticString = #filePath,
        line: UInt = #line,
        _ body: () throws -> Void
    ) {
        XCTAssertThrowsError(try body(), file: file, line: line) { error in
            XCTAssertEqual((error as? WebsiteBrowserTransferError)?.reason, reason, file: file, line: line)
        }
    }
}
