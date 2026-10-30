import FeedFlowKit
import Reader

extension ReaderFontOption {
    static func readerOptions(strings: FeedFlowStrings) -> [ReaderFontOption] {
        [
            ReaderFontFamily.system,
            ReaderFontFamily.literata,
            ReaderFontFamily.sourceSerif4,
            ReaderFontFamily.ibmPlexSans,
            ReaderFontFamily.atkinsonHyperlegibleNext
        ].map { font in
            ReaderFontOption(
                id: font.name,
                label: font.familyName ?? strings.readerFontSystem,
                fontName: ReaderFontRegistry.postScriptName(for: font),
                note: font.isLatinScriptOnly ? strings.readerFontLatinScriptOnly : nil
            )
        }
    }
}
