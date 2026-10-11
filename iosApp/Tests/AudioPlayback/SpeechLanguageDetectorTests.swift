import XCTest

@MainActor
final class SpeechLanguageDetectorTests: XCTestCase {
    func testEmptyTextHasNoDetectedLanguage() async {
        let language = await SpeechLanguageDetector.detect("")
        XCTAssertNil(language)
    }

    func testItalianArticleTextIsDetected() async {
        let text = """
        La lettura degli articoli è una parte importante della nostra giornata.
        Questo articolo racconta come le persone possono ascoltare le notizie nella propria lingua.
        Il lettore sceglie una voce italiana per rendere la pronuncia naturale e comprensibile.
        """
        let language = await SpeechLanguageDetector.detect(text)
        XCTAssertEqual(language, "it")
    }
}
