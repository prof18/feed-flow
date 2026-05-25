package com.prof18.feedflow.shared.ui.settings

internal object BlockedWordsE2eIds {
    const val INPUT = "blocked_words_input"
    const val ADD_BUTTON = "blocked_words_add_button"

    fun row(word: String): String =
        "blocked_word_${word.toE2eIdSuffix()}"

    fun deleteButton(word: String): String =
        "blocked_word_delete_${word.toE2eIdSuffix()}"
}

private fun String.toE2eIdSuffix(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') {
            char.lowercaseChar()
        } else {
            '_'
        }
    }.joinToString(separator = "")
