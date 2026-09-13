//
//  FeedContentFont.swift
//  FeedFlow
//

import CoreText
import FeedFlowKit
import SwiftUI

enum FeedContentFont {
    private static var registeredNames = [String: String]()

    static func title(family: ReaderFontFamily, size: CGFloat) -> Font {
        custom(family: family, size: size, bold: true) ?? .system(size: size).bold()
    }

    static func body(family: ReaderFontFamily, size: CGFloat) -> Font {
        custom(family: family, size: size, bold: false) ?? .system(size: size)
    }

    private static func custom(family: ReaderFontFamily, size: CGFloat, bold: Bool) -> Font? {
        guard family != .system else { return nil }
        let fileName: String?
        if bold, let boldName = family.boldResourceFileName() {
            fileName = boldName
        } else {
            fileName = family.resourceFileName()
        }
        guard let fileName else { return nil }
        guard let postScriptName = registeredFontName(for: fileName) else { return nil }
        return Font.custom(postScriptName, size: size)
            .weight(bold && family.boldResourceFileName() == nil ? .bold : .regular)
    }

    private static func registeredFontName(for fileName: String) -> String? {
        if let cached = registeredNames[fileName] {
            return cached
        }
        let baseName = (fileName as NSString).deletingPathExtension
        let ext = (fileName as NSString).pathExtension
        let url =
            Bundle.main.url(forResource: baseName, withExtension: ext, subdirectory: "reader-fonts")
            ?? Bundle.main.url(forResource: "reader-fonts/\(baseName)", withExtension: ext)
            ?? Bundle.main.url(forResource: baseName, withExtension: ext)
        guard let url else { return nil }

        var error: Unmanaged<CFError>?
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, &error)

        guard let descriptors = CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as? [CTFontDescriptor],
              let descriptor = descriptors.first,
              let name = CTFontDescriptorCopyAttribute(descriptor, kCTFontNameAttribute) as? String
        else {
            return nil
        }
        registeredNames[fileName] = name
        return name
    }
}
