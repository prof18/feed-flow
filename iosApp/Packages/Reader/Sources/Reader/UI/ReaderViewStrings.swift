import Foundation

public struct ReaderViewStrings {
    public let share: String
    public let addBookmark: String
    public let removeBookmark: String
    public let openInArchive: String
    public let openComments: String
    public let fontSize: String
    public let lineHeight: String
    public let textSettings: String
    public let resetToDefault: String
    public let done: String
    public let previousArticle: String
    public let nextArticle: String

    public init(
        share: String,
        addBookmark: String,
        removeBookmark: String,
        openInArchive: String,
        openComments: String,
        fontSize: String,
        lineHeight: String,
        textSettings: String,
        resetToDefault: String,
        done: String,
        previousArticle: String,
        nextArticle: String
    ) {
        self.share = share
        self.addBookmark = addBookmark
        self.removeBookmark = removeBookmark
        self.openInArchive = openInArchive
        self.openComments = openComments
        self.fontSize = fontSize
        self.lineHeight = lineHeight
        self.textSettings = textSettings
        self.resetToDefault = resetToDefault
        self.done = done
        self.previousArticle = previousArticle
        self.nextArticle = nextArticle
    }
}
