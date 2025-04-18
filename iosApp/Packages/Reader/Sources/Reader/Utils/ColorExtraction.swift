// From https://stackoverflow.com/questions/56586055/how-to-get-rgb-components-from-color-in-swiftui

import SwiftUI

#if os(macOS)
    import AppKit

// typealias UINSColor = NSColor
#else
    import UIKit

    // typealias UINSColor = UIColor
#endif

extension UINSColor {
    var components: (red: CGFloat, green: CGFloat, blue: CGFloat, opacity: CGFloat) {
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var o: CGFloat = 0
        #if os(macOS)
            usingColorSpace(.deviceRGB)!.getRed(&r, green: &g, blue: &b, alpha: &o)
        #else
            guard getRed(&r, green: &g, blue: &b, alpha: &o) else {
                // You can handle the failure here as you want
                return (0, 0, 0, 0)
            }
        #endif
        return (r, g, b, o)
    }

    // From https://stackoverflow.com/questions/26341008/how-to-convert-uicolor-to-hex-and-display-in-nslog
    var hexString: String {
        let (red, green, blue, _) = components
        let hexString = String(format: "#%02lX%02lX%02lX", lroundf(Float(red * 255)), lroundf(Float(green * 255)), lroundf(Float(blue * 255)))
        return hexString
    }

    var hexPair: (light: String, dark: String) {
        var light: String!
        var dark: String!
        withColorScheme(dark: false) {
            light = self.hexString
        }
        withColorScheme(dark: true) {
            dark = self.hexString
        }
        return (light, dark)
    }
}

private func withColorScheme(dark: Bool /* otherwise light */, block: () -> Void) {
    #if os(macOS)
        NSAppearance(named: dark ? .darkAqua : .aqua)!.performAsCurrentDrawingAppearance {
            block()
        }
    #else
        UITraitCollection(userInterfaceStyle: dark ? .dark : .light).performAsCurrent {
            block()
        }
    #endif
}
