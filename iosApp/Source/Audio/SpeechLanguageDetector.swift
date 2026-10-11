import Foundation
import NaturalLanguage

enum SpeechLanguageDetector {
    static func detect(_ text: String) async -> String? {
        await Task.detached(priority: .userInitiated) {
            guard !text.isEmpty else { return nil }
            let recognizer = NLLanguageRecognizer()
            recognizer.processString(text)
            let hypotheses = recognizer.languageHypotheses(withMaximum: 1)
            guard let (language, confidence) = hypotheses.first, confidence >= 0.8 else { return nil }
            return language.rawValue
        }.value
    }
}
