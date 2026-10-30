import CoreText
import SwiftUI
import UIKit

public struct ReaderFontOption: Identifiable, Hashable {
    public let id: String
    public let label: String
    /// PostScript name of a registered bundled font; nil means the system font.
    public let fontName: String?
    /// Shown under the picker while this option is selected.
    public let note: String?

    public init(id: String, label: String, fontName: String? = nil, note: String? = nil) {
        self.id = id
        self.label = label
        self.fontName = fontName
        self.note = note
    }

    func font(size: CGFloat, weight: Int) -> Font {
        guard let fontName else {
            return .system(size: size, weight: weight >= 600 ? .bold : .regular)
        }
        // Pinning the variable font's weight axis renders the real weight instead of a synthetic bold.
        let variationAttribute = UIFontDescriptor.AttributeName(rawValue: kCTFontVariationAttribute as String)
        let descriptor = UIFontDescriptor(fontAttributes: [
            .name: fontName,
            variationAttribute: [Self.weightAxisTag: weight]
        ])
        return Font(UIFont(descriptor: descriptor, size: size))
    }

    private static let weightAxisTag = 0x7767_6874 // 'wght'
}
