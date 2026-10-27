public struct ReaderViewScripts {
    public let fontSize: (Int) -> String
    public let lineHeight: (Int) -> String
    public let lineHeightLabel: (Int) -> String
    public let titleFontSize: (Int) -> String
    /// Builds the body/headline font script off the main thread: uncached fonts are read and encoded.
    public let fontFamily: @Sendable (String, String) async -> String
    public let stateUpdate: String?

    public init(
        fontSize: @escaping (Int) -> String,
        lineHeight: @escaping (Int) -> String,
        lineHeightLabel: @escaping (Int) -> String,
        fontFamily: @escaping @Sendable (String, String) async -> String,
        stateUpdate: String? = nil,
        titleFontSize: @escaping (Int) -> String = { _ in "" }
    ) {
        self.fontSize = fontSize
        self.lineHeight = lineHeight
        self.lineHeightLabel = lineHeightLabel
        self.titleFontSize = titleFontSize
        self.fontFamily = fontFamily
        self.stateUpdate = stateUpdate
    }
}
