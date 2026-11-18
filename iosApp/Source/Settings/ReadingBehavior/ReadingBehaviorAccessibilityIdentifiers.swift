enum ReadingBehaviorAccessibilityIdentifiers {
    static let articleOpenModePicker = "reading_behavior_article_open_mode"
    static let linksBrowserPicker = "reading_behavior_links_browser"
    static let articleOpenModeFullArticleOption = "article_open_mode_full_article"
    static let articleOpenModeFeedContentOption = "article_open_mode_feed_content"
    static let articleOpenModeInternalBrowserOption = "article_open_mode_internal_browser"
    static let markReadWhenScrollingToggle = "reading_behavior_mark_read_when_scrolling"
    static let readArticlesPicker = "reading_behavior_read_articles"
    static let readArticlesKeepOption = "reading_behavior_read_articles_keep"
    static let readArticlesHideOnRefreshOption = "reading_behavior_read_articles_hide_on_refresh"
    static let readArticlesHideRightAwayOption = "reading_behavior_read_articles_hide_right_away"

    static func articleOpenModeBrowserOption(_ browserId: String) -> String {
        "article_open_mode_browser_\(browserId.isEmpty ? "default" : browserId)"
    }
}
