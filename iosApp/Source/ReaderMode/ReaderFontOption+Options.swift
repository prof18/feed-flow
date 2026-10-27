import FeedFlowKit
import Reader

extension ReaderFontOption {
    static func readerOptions(strings: FeedFlowStrings) -> [ReaderFontOption] {
        [
            (ReaderFontFamily.system, strings.readerFontSystem),
            (ReaderFontFamily.literata, strings.readerFontLiterata),
            (ReaderFontFamily.sourceSerif4, strings.readerFontSourceSerif4),
            (ReaderFontFamily.lora, strings.readerFontLora),
            (ReaderFontFamily.atkinsonHyperlegibleNext, strings.readerFontAtkinsonHyperlegibleNext)
        ].map { font, label in
            ReaderFontOption(id: font.name, label: label, fontName: ReaderFontRegistry.postScriptName(for: font))
        }
    }
}
