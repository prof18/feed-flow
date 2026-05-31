enum FeedItemAccessibilityIdentifiers {
  static func row(_ feedItemId: String) -> String {
    "article_row_\(feedItemId.e2eIdSuffix)"
  }

  static func image(_ feedItemId: String) -> String {
    "article_image_\(feedItemId.e2eIdSuffix)"
  }
}

private extension String {
  var e2eIdSuffix: String {
    map { character in
      character.isLetter || character.isNumber || character == "_" ? character : "_"
    }
    .map(String.init)
    .joined()
  }
}
