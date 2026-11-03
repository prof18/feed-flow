import Foundation
import Nuke
import XCTest

final class SVGImageDecoderTests: XCTestCase {
    func testPlainSVGRootIsRecognized() {
        XCTAssertTrue(hasSignature("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"))
    }

    func testLeadingWhitespaceIsSkipped() {
        XCTAssertTrue(hasSignature("\n\n  <svg></svg>"))
    }

    func testXMLDeclarationIsSkipped() {
        XCTAssertTrue(hasSignature("<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg></svg>"))
    }

    func testGeneratorCommentIsSkipped() {
        XCTAssertTrue(hasSignature("<!-- Generator: Adobe Illustrator 27.0 --><svg></svg>"))
    }

    func testCommentContainingAngleBracketIsSkipped() {
        XCTAssertTrue(hasSignature("<!-- width > height, exported --><svg></svg>"))
    }

    func testDoctypeAndCommentAreSkipped() {
        let svg = """
        <?xml version="1.0"?>
        <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
        <!-- exported -->
        <svg width="18" height="18"></svg>
        """
        XCTAssertTrue(hasSignature(svg))
    }

    func testHTMLPageIsNotTreatedAsSVG() {
        XCTAssertFalse(hasSignature("<!DOCTYPE html><html><body>Not found</body></html>"))
    }

    func testHTMLEmbeddingAnInlineIconIsNotTreatedAsSVG() {
        XCTAssertFalse(hasSignature("<html><body><svg viewBox=\"0 0 1 1\"></svg></body></html>"))
    }

    func testBinaryDataIsNotTreatedAsSVG() {
        let png = Data([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01])
        XCTAssertFalse(SVGImageDecoder.hasSVGSignature(png))
    }

    func testUnterminatedCommentIsNotTreatedAsSVG() {
        XCTAssertFalse(hasSignature("<!-- never closed <svg></svg>"))
    }

    func testByteOrderMarkIsSkipped() {
        XCTAssertTrue(hasSignature("\u{FEFF}<?xml version=\"1.0\"?><svg></svg>"))
    }

    func testSmallIconIsRaisedToTheDefaultCap() {
        XCTAssertEqual(SVGImageDecoder.rasterSize(for: CGSize(width: 18, height: 18)),
                       CGSize(width: 128, height: 128))
    }

    func testLargeArtworkIsCappedAndKeepsItsAspectRatio() {
        XCTAssertEqual(SVGImageDecoder.rasterSize(for: CGSize(width: 1_200, height: 675)),
                       CGSize(width: 1_024, height: 576))
    }

    func testMidSizedArtworkIsRasterizedAtTwiceItsSize() {
        XCTAssertEqual(SVGImageDecoder.rasterSize(for: CGSize(width: 240, height: 120)),
                       CGSize(width: 480, height: 240))
    }

    func testAnExtremelyThinVectorKeepsAtLeastOnePixelPerSide() {
        // Past the cap the ratio shrinks below 1 and the short side would round to zero.
        XCTAssertEqual(SVGImageDecoder.rasterSize(for: CGSize(width: 4_000, height: 1)),
                       CGSize(width: 1_024, height: 1))
        XCTAssertEqual(SVGImageDecoder.rasterSize(for: CGSize(width: 1, height: 4_000)),
                       CGSize(width: 1, height: 1_024))
    }

    func testAnUnknownIntrinsicSizeFallsBackToTheDefaultCap() {
        XCTAssertEqual(SVGImageDecoder.rasterSize(for: .zero),
                       CGSize(width: 128, height: 128))
    }

    func testDoctypeWithAnInternalSubsetIsSkipped() {
        let svg = """
        <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "svg11.dtd" [
          <!ENTITY accent "#ff6600">
        ]>
        <svg width="18" height="18"></svg>
        """
        XCTAssertTrue(hasSignature(svg))
    }

    func testDecodingASizedSVGRasterizesForItsOwnSize() throws {
        let image = try SVGImageDecoder().decode(Data("""
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="10"><rect width="20" height="10"/></svg>
        """.utf8)).image
        XCTAssertEqual(image.size, CGSize(width: 128, height: 64))
    }

    func testDecodingAViewBoxOnlySVGUsesTheViewBoxAspectRatio() throws {
        let image = try SVGImageDecoder().decode(Data("""
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 20"><rect width="40" height="20"/></svg>
        """.utf8)).image
        XCTAssertEqual(image.size, CGSize(width: 128, height: 64))
    }

    func testDecodingUnparsableMarkupThrows() {
        XCTAssertThrowsError(try SVGImageDecoder().decode(Data("<svg".utf8)))
    }

    private func hasSignature(_ text: String) -> Bool {
        SVGImageDecoder.hasSVGSignature(Data(text.utf8))
    }
}
