import Foundation
import Nuke
import SwiftDraw
import UIKit

/// Nuke decodes through ImageIO, which has no SVG type.
struct SVGImageDecoder: ImageDecoding {
    /// Nuke keys the decode on the URL alone, so every request for one SVG shares a single raster:
    /// sizing it from the request would hand a 600pt hero the 24pt logo's thumbnail.
    private static let defaultRasterPixels: CGFloat = 128
    private static let maximumRasterPixels: CGFloat = 1_024
    private static let rasterScale: CGFloat = 2

    private static let prologScanBytes = 1_024

    private static let registration = ImageDecoderRegistry.shared.register { context in
        guard hasSVGSignature(context.data) else { return nil }
        return SVGImageDecoder()
    }

    /// The app and the widget are separate processes, so each registers on its own.
    static func register() {
        _ = registration
    }

    /// Skips any prolog. Requiring `<svg` to be the root keeps an HTML page that embeds an inline
    /// icon from being treated as an image.
    static func hasSVGSignature(_ data: Data) -> Bool {
        // Lenient: the prefix can cut a multi-byte character in half.
        // swiftlint:disable:next optional_data_string_conversion
        var text = String(decoding: data.prefix(prologScanBytes), as: UTF8.self)
            .drop { $0 == "\u{FEFF}" || $0.isWhitespace }

        while !text.isEmpty {
            if text.hasPrefix("<svg") {
                return true
            }
            if text.hasPrefix("<!--") {
                guard let close = text.range(of: "-->") else { return false }
                text = text[close.upperBound...].drop { $0.isWhitespace }
            } else if text.hasPrefix("<?") || text.hasPrefix("<!") {
                guard let afterClose = endOfDeclaration(in: text) else { return false }
                text = text[afterClose...].drop { $0.isWhitespace }
            } else {
                return false
            }
        }
        return false
    }

    /// The index past a declaration's closing `>`, ignoring one inside a doctype internal subset.
    private static func endOfDeclaration(in text: Substring) -> Substring.Index? {
        var depth = 0
        var index = text.startIndex
        while index < text.endIndex {
            switch text[index] {
            case "[":
                depth += 1
            case "]":
                depth = max(0, depth - 1)
            case ">" where depth == 0:
                return text.index(after: index)
            default:
                break
            }
            index = text.index(after: index)
        }
        return nil
    }

    /// Its own size at 2x, clamped between the default and the cap, never below a pixel a side:
    /// a side that rounded to zero would hand SwiftDraw an empty bitmap.
    static func rasterSize(for intrinsicSize: CGSize) -> CGSize {
        let longestSide = max(intrinsicSize.width, intrinsicSize.height)
        guard intrinsicSize.width > 0, intrinsicSize.height > 0, longestSide.isFinite else {
            return CGSize(width: defaultRasterPixels, height: defaultRasterPixels)
        }
        let target = min(max(longestSide * rasterScale, defaultRasterPixels), maximumRasterPixels)
        let ratio = target / longestSide
        return CGSize(
            width: max(1, (intrinsicSize.width * ratio).rounded()),
            height: max(1, (intrinsicSize.height * ratio).rounded())
        )
    }

    func decode(_ data: Data) throws -> ImageContainer {
        guard let svg = SVG(data: data) else {
            throw ImageDecodingError.unknown
        }
        // Without an explicit scale SwiftDraw reads UIScreen.main.scale through
        // DispatchQueue.main.sync, and Nuke decodes off the main thread.
        return ImageContainer(image: svg.rasterize(size: Self.rasterSize(for: svg.size), scale: 1))
    }
}
