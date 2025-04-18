import SwiftUI

public struct ReaderViewOptions {
    public var theme: ReaderTheme
    public var onLinkClicked: ((URL) -> Void)?
    public init(theme: ReaderTheme = .init(), onLinkClicked: ((URL) -> Void)? = nil) {
        self.theme = theme
        self.onLinkClicked = onLinkClicked
    }
}

public struct ReaderView<ToolbarView: View>: View {
    var url: URL
    var options: ReaderViewOptions
    @ToolbarContentBuilder let toolbarContent: () -> ToolbarView

    public init(url: URL, options: ReaderViewOptions = .init(), @ViewBuilder toolbarContent: @escaping () -> ToolbarView) {
        self.url = url
        self.options = options
        self.toolbarContent = toolbarContent
    }

    // MARK: - Implementation
    enum Status: Equatable {
        case fetching
        case failedToExtractContent
        case extractedContent(html: String, baseURL: URL, title: String?)
    }
    @State private var status = Status.fetching
    @State private var titleFromFallbackWebView: String?

    public var body: some View {
        Color(options.theme.background)
            .overlay(content)
            .overlay(loader)
            .navigationTitle(title ?? url.hostWithoutWWW)
        #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                toolbarContent()
            }
        #endif
            .task {
                do {
                    let result = try await Reader.fetchAndExtractContent(fromURL: url, theme: options.theme)
                    self.status = .extractedContent(html: result.styledHTML, baseURL: result.baseURL, title: result.title)
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
            FallbackWebView(url: url, onLinkClicked: onLinkClicked, title: $titleFromFallbackWebView)
        case .extractedContent(let html, let baseURL, _):
            ReaderWebView(baseURL: baseURL, html: html, onLinkClicked: onLinkClicked)
        }
    }

    // TODO: Show loader while fallback page is loading
    @ViewBuilder private var loader: some View {
        ReaderPlaceholder(theme: options.theme)
            .opacity(showLoader ? 1 : 0)
            .animation(.default, value: showLoader)
    }

    private var showLoader: Bool {
        status == .fetching
    }

    private var title: String? {
        switch status {
        case .fetching:
            return nil
        case .failedToExtractContent:
            return titleFromFallbackWebView
        case .extractedContent(_, _, let title):
            return title
        }
    }

    private func onLinkClicked(_ url: URL) {
        if url == .exitReaderModeLink {
            showNormalPage()
        } else {
            options.onLinkClicked?(url)
        }
    }

    private func showNormalPage() {
        status = .failedToExtractContent // TODO: Model this state correctly
    }
}

private struct FallbackWebView: View {
    var url: URL
    var onLinkClicked: ((URL) -> Void)?
    @Binding var title: String?

    @StateObject private var content = WebContent()

    var body: some View {
        WebView(content: content)
            .onAppear {
                setupLinkHandler()
            }
            .onAppearOrChange(url) { url in
                content.populate { content in
                    content.load(url: url)
                }
            }
            .onChange(of: content.info.title) { self.title = $0 }
    }

    private func setupLinkHandler() {
        content.shouldBlockNavigation = { action -> Bool in
            if action.navigationType == .linkActivated, let url = action.request.url {
                onLinkClicked?(url)
                return true
            }
            return false
        }
    }
}

private struct ReaderWebView: View {
    var baseURL: URL
    var html: String
    var onLinkClicked: ((URL) -> Void)?
    // TODO: Handle "wants to exit reader"

    @StateObject private var content = WebContent(transparent: true)

    var body: some View {
        WebView(content: content)
            .onAppear {
                setupLinkHandler()
            }
            .onAppearOrChange(Model(baseURL: baseURL, html: html)) { model in
                content.populate { content in
                    content.load(html: model.html, baseURL: model.baseURL)
                }
            }
    }

    private struct Model: Equatable {
        var baseURL: URL
        var html: String
    }

    private func setupLinkHandler() {
        content.shouldBlockNavigation = { action -> Bool in
            if let url = action.request.url,
                url == .exitReaderModeLink || action.navigationType == .linkActivated {
                onLinkClicked?(url)
                return true
            }
            return false
        }
    }
}

