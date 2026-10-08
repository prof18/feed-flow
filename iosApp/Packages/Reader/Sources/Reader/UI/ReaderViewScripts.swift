public struct ReaderViewScripts {
    public let fontSize: (Int) -> String
    public let lineHeight: (Int) -> String
    public let lineHeightLabel: (Int) -> String
    public let stateUpdate: String?

    public init(
        fontSize: @escaping (Int) -> String,
        lineHeight: @escaping (Int) -> String,
        lineHeightLabel: @escaping (Int) -> String,
        stateUpdate: String? = nil
    ) {
        self.fontSize = fontSize
        self.lineHeight = lineHeight
        self.lineHeightLabel = lineHeightLabel
        self.stateUpdate = stateUpdate
    }
}
