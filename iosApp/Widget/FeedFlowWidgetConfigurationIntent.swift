import AppIntents

struct FeedFlowWidgetConfigurationIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "FeedFlow"

    @Parameter(title: LocalizedStringResource("widget_content_section_title", table: "Widget"))
    var content: WidgetContentEntity?
}
