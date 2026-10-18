import SwiftUI
import UIKit

public struct ReaderView<BottomAccessory: View>: View {
    @Binding var readerStatus: ReaderStatus
    var options: ReaderViewOptions
    var themeColors: ReaderThemeColors
    var scripts: ReaderViewScripts
    var actions: ReaderViewActions
    var isBookmarked: Bool
    var fontSize: Double
    var lineHeight: Double
    var defaultFontSize: Double
    var defaultLineHeight: Double
    @Binding var showFontSizeMenu: Bool
    var openInBrowser: (URL) -> Void
    var bottomAccessory: BottomAccessory
    var isBottomAccessoryVisible: Bool

    @State private var webContent: WebContent?
    @State private var topContentInset: CGFloat = 0
    @State private var lastArticle: DisplayedArticle?
    @State private var lastUrl: URL?
    @State private var allowsPlaceholder = true
    @State private var placeholderDelay: Task<Void, Never>?

    private static var readerSpace: String { "readerBody" }

    private var isFetching: Bool {
        readerStatus == .fetching
    }

    /// While the next article loads, the url-based toolbar items keep the previous url (disabled)
    /// so the bars don't shrink and grow back on every Next/Previous.
    private var toolbarUrl: URL? {
        isFetching ? lastUrl : readerStatus.getUrl()
    }

    public init(
        readerStatus: Binding<ReaderStatus>,
        options: ReaderViewOptions,
        themeColors: ReaderThemeColors,
        scripts: ReaderViewScripts,
        actions: ReaderViewActions,
        isBookmarked: Bool,
        fontSize: Double,
        lineHeight: Double,
        defaultFontSize: Double,
        defaultLineHeight: Double,
        showFontSizeMenu: Binding<Bool>,
        openInBrowser: @escaping (URL) -> Void,
        isBottomAccessoryVisible: Bool = true,
        @ViewBuilder bottomAccessory: () -> BottomAccessory
    ) {
        _readerStatus = readerStatus
        self.options = options
        self.themeColors = themeColors
        self.scripts = scripts
        self.actions = actions
        self.isBookmarked = isBookmarked
        self.fontSize = fontSize
        self.lineHeight = lineHeight
        self.defaultFontSize = defaultFontSize
        self.defaultLineHeight = defaultLineHeight
        _showFontSizeMenu = showFontSizeMenu
        self.openInBrowser = openInBrowser
        self.bottomAccessory = bottomAccessory()
        self.isBottomAccessoryVisible = isBottomAccessoryVisible
    }

    public var body: some View {
        if #available(iOS 26, *) {
            readerBody
                .modifier(ReaderBottomAccessoryModifier(accessory: bottomAccessory, isVisible: isBottomAccessoryVisible))
        } else {
            readerBody
                .safeAreaInset(edge: .bottom, spacing: 0) {
                    VStack(spacing: 10) {
                        if isBottomAccessoryVisible {
                            bottomAccessory
                        }
                        compactNavigationToolbar
                    }
                }
        }
    }

    private var readerBody: some View {
        ZStack {
            Color(ReaderTheme.background)
                .overlay(content)
                // Measured against the safe-area-sized ZStack in one layout pass: how far the surface
                // reaches above it is the height of the bars it now runs beneath.
                .onGeometryChange(for: CGFloat.self) { proxy in
                    max(0, -proxy.frame(in: .named(Self.readerSpace)).minY)
                } action: { inset in
                    topContentInset = inset
                }
                // The web views inset their own content, so the article scrolls beneath the floating bars.
                .ignoresSafeArea(.container, edges: isiOS26OrLater() ? .vertical : [])
                .overlay(loader)
                .navigationBarTitleDisplayMode(.inline)
                .onChange(of: readerStatus) { _, status in
                    switch status {
                    case .contentUnavailable, .failedToExtractContent:
                        lastArticle = nil
                    case .fetching:
                        delayPlaceholderIfSwitching()
                    case .extractedContent:
                        break
                    }
                    if !isFetching {
                        lastUrl = status.getUrl()
                    }
                }
                .onAppear {
                    if !isFetching {
                        lastUrl = readerStatus.getUrl()
                    }
                }
                .onChange(of: themeColors) { _, newValue in
                    applyThemeWithJS(newValue)
                }
                .onChange(of: scripts.stateUpdate) { _, script in
                    if let webContent {
                        applyStateUpdate(script, to: webContent)
                    }
                }
                .sheet(isPresented: $showFontSizeMenu) {
                    fontSizeSheet
                        .presentationDetents([.medium])
                        .presentationBackground(Color(.secondarySystemBackground))
                }
        }
        .coordinateSpace(.named(Self.readerSpace))
        .toolbar {
            if isiOS26OrLater() {
                makeIOS26ToolbarContent()
            } else {
                makeLegacyToolbarContent()
            }
        }
        .modifier(ReaderFixedToolbarModifier())
    }

    /// The article the reader web view shows. While the next article is fetched it keeps the last
    /// one, so Next/Previous reuse the same web view (and its WebKit process) instead of creating
    /// one per article; the loader covers it in the meantime.
    private var displayedArticle: DisplayedArticle? {
        switch readerStatus {
        case let .extractedContent(html, baseURL, _, contentId):
            DisplayedArticle(html: html, baseURL: baseURL, contentId: contentId)
        case .fetching:
            lastArticle
        case .contentUnavailable, .failedToExtractContent:
            nil
        }
    }

    /// The reader web view is also mounted, still empty, during the first fetch, so WebKit starts
    /// up while the article is extracted.
    private var showsReaderWebView: Bool {
        displayedArticle != nil || readerStatus == .fetching
    }

    @ViewBuilder private var content: some View {
        if showsReaderWebView {
            let article = displayedArticle
            ReaderWebView(
                article: article,
                allowsPlaceholder: allowsPlaceholder,
                onLinkClicked: onLinkClicked,
                onImageClicked: options.onImageClicked,
                topInset: topContentInset,
                onWebContentReady: { content in
                    webContent = content
                    applyThemeWithJS(themeColors)
                    applyStateUpdate(scripts.stateUpdate, to: content)
                }
            )
            .onChange(of: article) { _, article in
                lastArticle = article
            }
            .onAppear {
                lastArticle = article
            }
        } else {
            switch readerStatus {
            case .fetching, .extractedContent:
                EmptyView()
            case .contentUnavailable:
                contentUnavailableView
            case let .failedToExtractContent(url):
                FallbackWebView(
                    url: url,
                    onLinkClicked: onLinkClicked,
                    topInset: topContentInset
                )
            }
        }
    }

    private var contentUnavailableView: some View {
        VStack(spacing: 8) {
            Text(actions.strings.contentUnavailableTitle)
                .font(.headline)
                .multilineTextAlignment(.center)

            Text(actions.strings.contentUnavailableMessage)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var loader: some View {
        let isVisible = isFetching && allowsPlaceholder
        return ReaderPlaceholder()
            .opacity(isVisible ? 1 : 0)
            .animation(.default, value: isVisible)
    }

    /// On Next/Previous the current article stays up for a moment: if the next one is ready quickly
    /// the reader switches straight to it, and the placeholder only appears for a slower load.
    private func delayPlaceholderIfSwitching() {
        placeholderDelay?.cancel()
        guard lastArticle != nil else {
            allowsPlaceholder = true
            return
        }
        allowsPlaceholder = false
        placeholderDelay = Task {
            try? await Task.sleep(for: .milliseconds(150))
            guard !Task.isCancelled else { return }
            allowsPlaceholder = true
        }
    }

    private func onLinkClicked(_ url: URL) {
        options.onLinkClicked?(url)
    }

    private func applyThemeWithJS(_ colors: ReaderThemeColors) {
        guard let webContent = webContent else { return }
        let script = """
            document.documentElement.style.setProperty("--reader-text", "\(colors.textColor)");
            document.documentElement.style.setProperty("--reader-link", "\(colors.linkColor)");
            document.documentElement.style.setProperty("--reader-bg", "\(colors.backgroundColor)");
            document.documentElement.style.setProperty("--reader-border", "\(colors.borderColor)");
        """
        webContent.evaluateJavaScript(script)
    }

    private func applyStateUpdate(_ script: String?, to content: WebContent) {
        content.onContentReady = { [weak content] in
            if let script {
                content?.evaluateJavaScript(script)
            }
        }
        if let script {
            content.evaluateJavaScript(script)
        }
    }

    @ToolbarContentBuilder
    private func makeIOS26ToolbarContent() -> some ToolbarContent {
        // Bottom bar - left edge
        if actions.hasUrl, let url = toolbarUrl {
            ToolbarItem(placement: .bottomBar) {
                Button {
                    openInBrowser(url)
                } label: {
                    Label(actions.strings.openInBrowser, systemImage: "globe")
                }
                .disabled(isFetching)
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.browserButton)
            }
        }

        // Bottom bar - right edge (navigation arrows)
        if #available(iOS 26.0, *) {
            ToolbarSpacer(.flexible, placement: .bottomBar)
        }

        ToolbarItemGroup(placement: .bottomBar) {
            Button {
                actions.onNavigateToPrevious?()
            } label: {
                Label(actions.strings.previousArticle, systemImage: "chevron.left")
            }
            .disabled(actions.onNavigateToPrevious == nil)
            .accessibilityIdentifier(ReaderAccessibilityIdentifiers.previousButton)

            Button {
                actions.onNavigateToNext?()
            } label: {
                Label(actions.strings.nextArticle, systemImage: "chevron.right")
            }
            .disabled(actions.onNavigateToNext == nil)
            .accessibilityIdentifier(ReaderAccessibilityIdentifiers.nextButton)
        }

        // Top bar: Share button
        if actions.hasUrl, let url = toolbarUrl {
            if #available(iOS 26.0, *) {
                ToolbarSpacer(.fixed)
            }

            ToolbarItem(placement: .navigationBarTrailing) {
                ShareLink(
                    item: url,
                    label: {
                        Label(actions.strings.share, systemImage: "square.and.arrow.up")
                    }
                )
                .disabled(isFetching)
            }

            if #available(iOS 26.0, *) {
                ToolbarSpacer(.fixed)
            }
        }

        // Top bar: Menu with other actions
        ToolbarItem(placement: .primaryAction) {
            Menu {
                Button {
                    let newBookmarkState = !isBookmarked
                    actions.onBookmarkToggle(newBookmarkState)
                } label: {
                    Label(
                        isBookmarked ? actions.strings.removeBookmark : actions.strings.addBookmark,
                        systemImage: isBookmarked ? "bookmark.slash" : "bookmark"
                    )
                }
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.bookmarkButton)

                if let onReportArticle = actions.onReportArticle {
                    Button(action: onReportArticle) {
                        Label(actions.strings.reportArticle, systemImage: "exclamationmark.bubble")
                    }
                    .disabled(actions.isSendingReport)
                    .accessibilityIdentifier(ReaderReportAccessibilityIdentifiers.action)
                }

                if actions.hasUrl {
                    Button {
                        actions.onArchive()
                    } label: {
                        Label(actions.strings.openInArchive, systemImage: "lock.open")
                    }
                }

                if case .extractedContent = readerStatus,
                   let onComments = actions.onComments
                {
                    Button {
                        onComments()
                    } label: {
                        Label(actions.strings.openComments, systemImage: "bubble.left")
                    }
                }

                if case .extractedContent = readerStatus {
                    if let onToggleSpeech = actions.onToggleSpeech {
                        Button {
                            onToggleSpeech()
                        } label: {
                            Label(
                                speechActionTitle,
                                systemImage: actions.isSpeechPlaying ? "pause.fill" : "headphones"
                            )
                        }
                        .disabled(actions.isPreparingSpeech)
                        .accessibilityIdentifier(ReaderAccessibilityIdentifiers.ttsButton)
                    }

                    Button {
                        actions.onFontSizeMenuToggle()
                    } label: {
                        Label(actions.strings.textSettings, systemImage: "textformat.size")
                    }
                    .accessibilityIdentifier(ReaderAccessibilityIdentifiers.fontSizeButton)
                }

                if case .extractedContent = readerStatus,
                   let onToggleContentSource = actions.onToggleContentSource
                {
                    contentSourceMenuToggle(onToggle: onToggleContentSource)
                }
            } label: {
                Label(actions.strings.moreOptions, systemImage: "ellipsis.circle")
                    .accessibilityIdentifier(ReaderAccessibilityIdentifiers.moreMenuButton)
            }
        }
    }

    @ToolbarContentBuilder
    private func makeLegacyToolbarContent() -> some ToolbarContent {
        // Share button
        if actions.hasUrl, let url = toolbarUrl {
            ToolbarItem(placement: .navigationBarTrailing) {
                ShareLink(
                    item: url,
                    label: {
                        Label(actions.strings.share, systemImage: "square.and.arrow.up")
                    }
                )
                .disabled(isFetching)
            }
        }

        // Menu with other actions
        ToolbarItem(placement: .primaryAction) {
            Menu {
                Button {
                    let newBookmarkState = !isBookmarked
                    actions.onBookmarkToggle(newBookmarkState)
                } label: {
                    Label(
                        isBookmarked ? actions.strings.removeBookmark : actions.strings.addBookmark,
                        systemImage: isBookmarked ? "bookmark.slash" : "bookmark"
                    )
                }
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.bookmarkButton)

                if let onReportArticle = actions.onReportArticle {
                    Button(action: onReportArticle) {
                        Label(actions.strings.reportArticle, systemImage: "exclamationmark.bubble")
                    }
                    .disabled(actions.isSendingReport)
                    .accessibilityIdentifier(ReaderReportAccessibilityIdentifiers.action)
                }

                if actions.hasUrl {
                    Button {
                        actions.onArchive()
                    } label: {
                        Label(actions.strings.openInArchive, systemImage: "lock.open")
                    }
                }

                if case .extractedContent = readerStatus,
                   let onComments = actions.onComments
                {
                    Button {
                        onComments()
                    } label: {
                        Label(actions.strings.openComments, systemImage: "bubble.left")
                    }
                }

                if case .extractedContent = readerStatus {
                    if let onToggleSpeech = actions.onToggleSpeech {
                        Button {
                            onToggleSpeech()
                        } label: {
                            Label(
                                speechActionTitle,
                                systemImage: actions.isSpeechPlaying ? "pause.fill" : "headphones"
                            )
                        }
                        .disabled(actions.isPreparingSpeech)
                        .accessibilityIdentifier(ReaderAccessibilityIdentifiers.ttsButton)
                    }

                    Button {
                        actions.onFontSizeMenuToggle()
                    } label: {
                        Label(actions.strings.textSettings, systemImage: "textformat.size")
                    }
                    .accessibilityIdentifier(ReaderAccessibilityIdentifiers.fontSizeButton)
                }

                if case .extractedContent = readerStatus,
                   let onToggleContentSource = actions.onToggleContentSource
                {
                    contentSourceMenuToggle(onToggle: onToggleContentSource)
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .accessibilityIdentifier(ReaderAccessibilityIdentifiers.moreMenuButton)
            }
        }
    }

    private func contentSourceMenuToggle(onToggle: @escaping () -> Void) -> some View {
        Toggle(isOn: contentSourceToggleBinding(onToggle: onToggle)) {
            Label {
                Text(actions.strings.feedContent)
            } icon: {
                contentSourceMenuIcon
            }
        }
        .accessibilityIdentifier(ReaderAccessibilityIdentifiers.contentSourceButton)
    }

    private func contentSourceToggleBinding(onToggle: @escaping () -> Void) -> Binding<Bool> {
        Binding(
            get: { actions.isShowingFeedContent },
            set: { _ in onToggle() }
        )
    }

    private var speechActionTitle: String {
        if actions.isPreparingSpeech {
            return actions.strings.ttsPreparing
        }
        return actions.isSpeechPlaying ? actions.strings.ttsStop : actions.strings.ttsListen
    }

    @ViewBuilder
    private var contentSourceMenuIcon: some View {
        if let image = UIImage(systemName: "dot.radiowaves.up.forward")?
            .withTintColor(.label, renderingMode: .alwaysOriginal)
        {
            Image(uiImage: image)
        } else {
            Image(systemName: "dot.radiowaves.up.forward")
                .foregroundStyle(.primary)
        }
    }

    private var fontSizeSheet: some View {
        NavigationStack {
            VStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 22) {
                    textSettingSliderRow(
                        title: actions.strings.fontSize,
                        valueLabel: fontSizeValueLabel(fontSize),
                        valueAccessibilityIdentifier: ReaderAccessibilityIdentifiers.fontSizeValueLabel,
                        value: fontSize,
                        range: 12 ... 40,
                        step: 1,
                        onValueChange: {
                            updateFontSizeWithJS($0)
                            actions.onFontSizeChange($0)
                        }
                    )

                    Divider()

                    textSettingSliderRow(
                        title: actions.strings.lineHeight,
                        valueLabel: lineHeightValueLabel(lineHeight),
                        valueAccessibilityIdentifier: ReaderAccessibilityIdentifiers.lineHeightValueLabel,
                        value: lineHeight,
                        range: 0 ... 15,
                        step: 1,
                        onValueChange: {
                            updateLineHeightWithJS($0)
                            actions.onLineHeightChange($0)
                        }
                    )

                    HStack {
                        Spacer()
                        resetTextSettingsButton
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 56)

                Spacer(minLength: 0)
            }
            .background(Color(.secondarySystemBackground))
            .navigationTitle(actions.strings.textSettings)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showFontSizeMenu = false
                    } label: {
                        Text(actions.strings.done).bold()
                    }
                }
            }
        }
        .accessibilityIdentifier(ReaderAccessibilityIdentifiers.textSettingsSheet)
    }

    private var resetTextSettingsButton: some View {
        let isDisabled = fontSize == defaultFontSize && lineHeight == defaultLineHeight
        return Button {
            updateFontSizeWithJS(defaultFontSize)
            updateLineHeightWithJS(defaultLineHeight)
            actions.onFontSizeChange(defaultFontSize)
            actions.onLineHeightChange(defaultLineHeight)
        } label: {
            Text(actions.strings.resetToDefault)
                .font(.body)
        }
        .buttonStyle(.plain)
        .foregroundStyle(isDisabled ? Color.secondary : Color.accentColor)
        .disabled(isDisabled)
        .accessibilityIdentifier(ReaderAccessibilityIdentifiers.textSettingsResetButton)
    }

    private func textSettingSliderRow(
        title: String,
        valueLabel: String,
        valueAccessibilityIdentifier: String,
        value: Double,
        range: ClosedRange<Double>,
        step: Double?,
        onValueChange: @escaping (Double) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                Spacer()
                Text(valueLabel)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .accessibilityIdentifier(valueAccessibilityIdentifier)
            }

            if let step {
                Slider(
                    value: Binding(
                        get: { value },
                        set: onValueChange
                    ),
                    in: range,
                    step: step
                )
            } else {
                Slider(
                    value: Binding(
                        get: { value },
                        set: onValueChange
                    ),
                    in: range
                )
            }
        }
        .padding(.vertical, 4)
    }

    private func fontSizeValueLabel(_ fontSize: Double) -> String {
        "\(Int(fontSize))"
    }

    private func lineHeightValueLabel(_ lineHeight: Double) -> String {
        scripts.lineHeightLabel(Int(lineHeight))
    }

    private func updateFontSizeWithJS(_ newFontSize: Double) {
        guard let webContent = webContent else { return }
        webContent.evaluateJavaScript(scripts.fontSize(Int(newFontSize)))
    }

    private func updateLineHeightWithJS(_ lineHeight: Double) {
        guard let webContent = webContent else { return }
        webContent.evaluateJavaScript(scripts.lineHeight(Int(lineHeight)))
    }

    private var compactNavigationToolbar: some View {
        HStack(spacing: 0) {
            // Open in browser - left edge
            if actions.hasUrl, let url = toolbarUrl {
                Button {
                    openInBrowser(url)
                } label: {
                    Image(systemName: "globe")
                        .font(.title3)
                        .foregroundStyle(.primary)
                        .frame(width: 50, height: 44)
                }
                .background(.regularMaterial)
                .clipShape(Capsule())
                .shadow(radius: 8)
                .disabled(isFetching)
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.browserButton)
            }

            Spacer()

            // Navigation arrows - right edge
            HStack(spacing: 0) {
                Button {
                    actions.onNavigateToPrevious?()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.title3)
                        .foregroundStyle(.primary)
                        .frame(width: 50, height: 44)
                }
                .disabled(actions.onNavigateToPrevious == nil)
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.previousButton)

                Button {
                    actions.onNavigateToNext?()
                } label: {
                    Image(systemName: "chevron.right")
                        .font(.title3)
                        .foregroundStyle(.primary)
                        .frame(width: 50, height: 44)
                }
                .disabled(actions.onNavigateToNext == nil)
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.nextButton)
            }
            .background(.regularMaterial)
            .clipShape(Capsule())
            .shadow(radius: 8)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }
}

public extension ReaderView where BottomAccessory == EmptyView {
    init(
        readerStatus: Binding<ReaderStatus>,
        options: ReaderViewOptions,
        themeColors: ReaderThemeColors,
        scripts: ReaderViewScripts,
        actions: ReaderViewActions,
        isBookmarked: Bool,
        fontSize: Double,
        lineHeight: Double,
        defaultFontSize: Double,
        defaultLineHeight: Double,
        showFontSizeMenu: Binding<Bool>,
        openInBrowser: @escaping (URL) -> Void
    ) {
        self.init(
            readerStatus: readerStatus,
            options: options,
            themeColors: themeColors,
            scripts: scripts,
            actions: actions,
            isBookmarked: isBookmarked,
            fontSize: fontSize,
            lineHeight: lineHeight,
            defaultFontSize: defaultFontSize,
            defaultLineHeight: defaultLineHeight,
            showFontSizeMenu: showFontSizeMenu,
            openInBrowser: openInBrowser,
            isBottomAccessoryVisible: false,
            bottomAccessory: { EmptyView() }
        )
    }
}
