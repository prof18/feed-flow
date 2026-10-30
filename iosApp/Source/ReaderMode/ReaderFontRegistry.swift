import CoreGraphics
import CoreText
import FeedFlowKit
import Foundation

/// Registers the bundled reader fonts with Core Text so native views (the text settings preview)
/// can draw them; the reader web page loads the same files through CSS.
enum ReaderFontRegistry {
    private static let postScriptNames: [String: String] = registerBundledFonts()

    static func postScriptName(for font: ReaderFontFamily) -> String? {
        guard let fileName = font.fileName else { return nil }
        return postScriptNames[fileName]
    }

    // iosApp/project.yml copies shared/src/commonMain into the app bundle as the `commonMain` folder.
    private static func registerBundledFonts() -> [String: String] {
        guard let fontsUrl = Bundle.main.resourceURL?
            .appendingPathComponent("commonMain/resources/reader-fonts") else { return [:] }
        let fileNames = [
            ReaderFontFamily.literata,
            ReaderFontFamily.sourceSerif4,
            ReaderFontFamily.ibmPlexSans,
            ReaderFontFamily.atkinsonHyperlegibleNext
        ].compactMap(\.fileName)

        var names: [String: String] = [:]
        for fileName in fileNames {
            guard let provider = CGDataProvider(url: fontsUrl.appendingPathComponent(fileName) as CFURL),
                  let cgFont = CGFont(provider),
                  let postScriptName = cgFont.postScriptName as String?
            else { continue }
            // Registering twice fails harmlessly; the font stays usable either way.
            CTFontManagerRegisterGraphicsFont(cgFont, nil)
            names[fileName] = postScriptName
        }
        return names
    }
}
