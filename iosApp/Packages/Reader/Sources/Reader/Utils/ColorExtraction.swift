// From https://stackoverflow.com/questions/56586055/how-to-get-rgb-components-from-color-in-swiftui

import SwiftUI
import UIKit

extension UIColor {
    // From https://stackoverflow.com/questions/26341008/how-to-convert-uicolor-to-hex-and-display-in-nslog
    var hexString: String {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var opacity: CGFloat = 0
        guard getRed(&red, green: &green, blue: &blue, alpha: &opacity) else {
            return ""
        }

        let hexString = String(
            format: "#%02lX%02lX%02lX", lroundf(Float(red * 255)), lroundf(Float(green * 255)),
            lroundf(Float(blue * 255))
        )
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
    UITraitCollection(userInterfaceStyle: dark ? .dark : .light).performAsCurrent {
        block()
    }
}
