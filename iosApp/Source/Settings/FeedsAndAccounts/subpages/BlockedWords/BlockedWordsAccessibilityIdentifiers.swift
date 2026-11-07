enum BlockedWordsAccessibilityIdentifiers {
    static let input = "blocked_words_input"
    static let addButton = "blocked_words_add_button"

    static func row(_ word: String) -> String {
        "blocked_word_\(word.e2eIdSuffix)"
    }

    static func deleteButton(_ word: String) -> String {
        "blocked_word_delete_\(word.e2eIdSuffix)"
    }
}

private extension String {
    var e2eIdSuffix: String {
        lowercased()
            .map { character in
                character.isLetter || character.isNumber || character == "_" ? character : "_"
            }
            .map(String.init)
            .joined()
    }
}
