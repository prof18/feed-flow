import SwiftUI

#if os(macOS)
    import AppKit

    public typealias UINSColor = NSColor
#else
    import UIKit

    public typealias UINSColor = UIColor
#endif

public struct ReaderTheme {
    public var foreground: UINSColor // for body text
    public var foreground2: UINSColor // used for button titles
    public var background: UINSColor // page background
    public var background2: UINSColor // used for buttons
    public var link: UINSColor
    public var additionalCSS: String?

    public init(
        foreground: UINSColor = .reader_Primary,
        foreground2: UINSColor = .reader_Secondary,
        background: UINSColor = .reader_Background,
        background2: UINSColor = .reader_Background2,
        link: UINSColor = .systemBlue,
        additionalCSS: String? = nil
    ) {
        self.foreground = foreground
        self.foreground2 = foreground2
        self.background = background
        self.background2 = background2
        self.link = link
        self.additionalCSS = additionalCSS
    }
}

public extension UINSColor {
    #if os(macOS)
        static let reader_Primary = NSColor.labelColor
        static let reader_Secondary = NSColor.secondaryLabelColor
        static let reader_Background = NSColor.textBackgroundColor
        static let reader_Background2 = NSColor.windowBackgroundColor
    #else
        static let reader_Primary = UIColor.label
        static let reader_Secondary = UIColor.secondaryLabel
        static let reader_Background = UIColor.systemBackground
        static let reader_Background2 = UIColor.secondarySystemBackground
    #endif
}
