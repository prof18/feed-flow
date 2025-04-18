import SwiftUI

public struct ReaderView<ToolbarView: View>: View {
    var url: URL
    var options: ReaderViewOptions

    @ToolbarContentBuilder
    let toolbarContent: () -> ToolbarView

    @State private var status = ReaderStatus.fetching
    @State private var titleFromFallbackWebView: String?

    private var title: String? {
        switch status {
        case .fetching:
            return nil
        case .failedToExtractContent:
            return titleFromFallbackWebView
        case let .extractedContent(_, _, title):
            return title
        }
    }

    public init(
        url: URL, options: ReaderViewOptions = .init(),
        @ViewBuilder toolbarContent: @escaping () -> ToolbarView
    ) {
        self.url = url
        self.options = options
        self.toolbarContent = toolbarContent
    }

    public var body: some View {
        Color(ReaderTheme.background)
            .overlay(content)
            .overlay(loader)
            .navigationTitle(title ?? url.hostWithoutWWW)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                toolbarContent()
            }
            .task {
                do {
                    let result = try await Reader.fetchAndExtractContent(
                        fromURL: url, additionalCSS: options.additionalCSS
                    )
                    self.status = .extractedContent(
                        html: result.styledHTML,
                        baseURL: result.baseURL,
                        title: result.title
                    )
                } catch {
                    status = .failedToExtractContent
                }
            }
    }

    @ViewBuilder private var content: some View {
        switch status {
        case .fetching:
            EmptyView()
        case .failedToExtractContent:
            FallbackWebView(
                url: url,
                onLinkClicked: onLinkClicked,
                title: $titleFromFallbackWebView
            )
        case let .extractedContent(html, baseURL, _):
            ReaderWebView(baseURL: baseURL, html: html, onLinkClicked: onLinkClicked)
        }
    }

    @ViewBuilder private var loader: some View {
        ReaderPlaceholder()
            .opacity(status == .fetching ? 1 : 0)
            .animation(.default, value: status == .fetching)
    }

    private func onLinkClicked(_ url: URL) {
        if url == .exitReaderModeLink {
            status = .failedToExtractContent
        } else {
            options.onLinkClicked?(url)
        }
    }
}
