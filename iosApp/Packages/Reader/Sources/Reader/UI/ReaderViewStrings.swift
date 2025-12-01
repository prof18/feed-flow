import Foundation

public struct ReaderViewStrings {
    public let share: String
    public let addBookmark: String
    public let removeBookmark: String
    public let openInArchive: String
    public let openComments: String
    public let fontSize: String
    public let previousArticle: String
    public let nextArticle: String

    public init(
        share: String,
        addBookmark: String,
        removeBookmark: String,
        openInArchive: String,
        openComments: String,
        fontSize: String,
        previousArticle: String,
        nextArticle: String
    ) {
        self.share = share
        self.addBookmark = addBookmark
        self.removeBookmark = removeBookmark
        self.openInArchive = openInArchive
        self.openComments = openComments
        self.fontSize = fontSize
        self.previousArticle = previousArticle
        self.nextArticle = nextArticle
    }
}
