public struct ReaderThemeColors: Equatable {
    public let textColor: String
    public let linkColor: String
    public let backgroundColor: String
    public let borderColor: String

    public init(
        textColor: String,
        linkColor: String,
        backgroundColor: String,
        borderColor: String
    ) {
        self.textColor = textColor
        self.linkColor = linkColor
        self.backgroundColor = backgroundColor
        self.borderColor = borderColor
    }
}
