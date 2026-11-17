enum ReadingBehaviorAccessibilityIdentifiers {
    static let hideReadToggle = "reading_behavior_hide_read"
    static let articleOpenModePicker = "reading_behavior_article_open_mode"
    static let linksBrowserPicker = "reading_behavior_links_browser"
    static let articleOpenModeFullArticleOption = "article_open_mode_full_article"
    static let articleOpenModeFeedContentOption = "article_open_mode_feed_content"
    static let articleOpenModeInternalBrowserOption = "article_open_mode_internal_browser"
    static let markReadWhenScrollingToggle = "reading_behavior_mark_read_when_scrolling"
    static let showReadToggle = "reading_behavior_show_read"

    static func articleOpenModeBrowserOption(_ browserId: String) -> String {
        "article_open_mode_browser_\(browserId.isEmpty ? "default" : browserId)"
    }
}
