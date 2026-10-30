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
    var headlineFontId: String
    var bodyFontId: String
    var defaultHeadlineFontId: String
    var defaultBodyFontId: String
    var titleFontSize: Double
    var defaultTitleFontSize: Double
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
    @State private var fontScriptTask: Task<Void, Never>?
    // Opens tall so the preview, both font rows and the sliders fit without scrolling.
    @State private var textSettingsDetent: PresentationDetent = .large

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
        headlineFontId: String,
        bodyFontId: String,
        defaultHeadlineFontId: String,
        defaultBodyFontId: String,
        titleFontSize: Double = 32,
        defaultTitleFontSize: Double = 32,
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
        self.headlineFontId = headlineFontId
        self.bodyFontId = bodyFontId
        self.defaultHeadlineFontId = defaultHeadlineFontId
        self.defaultBodyFontId = defaultBodyFontId
        self.titleFontSize = titleFontSize
        self.defaultTitleFontSize = defaultTitleFontSize
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
                        .presentationDetents([.medium, .large], selection: $textSettingsDetent)
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
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    textSettingsPreview

                    fontFamilyTileRow(
                        title: actions.strings.font,
                        selection: bodyFontId,
                        accessibilityIdentifier: ReaderAccessibilityIdentifiers.fontPicker,
                        onSelectionChange: { newFontId in
                            updateFontFamilyWithJS(bodyFontId: newFontId, headlineFontId: newFontId)
                            actions.onHeadlineFontChange(newFontId)
                            actions.onBodyFontChange(newFontId)
                        }
                    )

                    VStack(spacing: 0) {
                        textSettingSliderRow(
                            title: actions.strings.fontSize,
                            valueLabel: fontSizeValueLabel(fontSize),
                            valueAccessibilityIdentifier: ReaderAccessibilityIdentifiers.fontSizeValueLabel,
                            value: fontSize,
                            range: 12 ... 40,
                            minimumIcon: "textformat.size.smaller",
                            maximumIcon: "textformat.size.larger",
                            onValueChange: {
                                updateFontSizeWithJS($0)
                                actions.onFontSizeChange($0)
                            }
                        )

                        Divider().padding(.leading, 16)

                        textSettingSliderRow(
                            title: actions.strings.titleFontSize,
                            valueLabel: fontSizeValueLabel(titleFontSize),
                            valueAccessibilityIdentifier: ReaderAccessibilityIdentifiers.titleFontSizeValueLabel,
                            value: titleFontSize,
                            range: 16 ... 48,
                            minimumIcon: "textformat.size.smaller",
                            maximumIcon: "textformat.size.larger",
                            onValueChange: {
                                updateTitleFontSizeWithJS($0)
                                actions.onTitleFontSizeChange($0)
                            }
                        )

                        Divider().padding(.leading, 16)

                        textSettingSliderRow(
                            title: actions.strings.lineHeight,
                            valueLabel: lineHeightValueLabel(lineHeight),
                            valueAccessibilityIdentifier: ReaderAccessibilityIdentifiers.lineHeightValueLabel,
                            value: lineHeight,
                            range: 0 ... 15,
                            minimumIcon: "arrow.down.and.line.horizontal.and.arrow.up",
                            maximumIcon: "arrow.up.and.line.horizontal.and.arrow.down",
                            onValueChange: {
                                updateLineHeightWithJS($0)
                                actions.onLineHeightChange($0)
                            }
                        )
                    }
                    .background(
                        Color(.tertiarySystemBackground),
                        in: RoundedRectangle(cornerRadius: 20, style: .continuous)
                    )
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 24)
            }
            .background(Color(.secondarySystemBackground))
            .navigationTitle(actions.strings.textSettings)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    resetTextSettingsButton
                }
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

    private func fontOption(id: String) -> ReaderFontOption? {
        actions.fontOptions.first { $0.id == id }
    }

    private func readerFont(id: String, size: Double, weight: Int) -> Font {
        fontOption(id: id)?.font(size: size, weight: weight) ?? .system(size: size)
    }

    // Fixed height, so dragging a size slider never moves the slider under the finger.
    private var textSettingsPreview: some View {
        VStack(alignment: .leading, spacing: titleFontSize * 0.35) {
            Text(actions.strings.previewTitle)
                .font(readerFont(id: headlineFontId, size: titleFontSize, weight: 700))
                // A larger title gets one line so the body stays visible in the fixed-height preview.
                .lineLimit(titleFontSize > defaultTitleFontSize ? 1 : 2)
                .fixedSize(horizontal: false, vertical: true)
            Text(actions.strings.previewBody)
                .font(readerFont(id: bodyFontId, size: fontSize, weight: 400))
                // The page uses line-height 1.5 + step / 10; lineSpacing adds to the ~1.2 natural line height.
                .lineSpacing(fontSize * (0.3 + lineHeight / 10))
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(Color(ReaderTheme.foreground))
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .padding(20)
        .frame(height: 230, alignment: .top)
        .clipped()
        .mask(
            LinearGradient(
                stops: [.init(color: .black, location: 0.75), .init(color: .clear, location: 1)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .background(
            Color(ReaderTheme.background),
            in: RoundedRectangle(cornerRadius: 20, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(Color(.separator).opacity(0.5), lineWidth: 0.5)
        )
        .animation(.snappy(duration: 0.2), value: headlineFontId)
        .animation(.snappy(duration: 0.2), value: bodyFontId)
        .accessibilityHidden(true)
    }

    private var resetTextSettingsButton: some View {
        let isDisabled = fontSize == defaultFontSize &&
            lineHeight == defaultLineHeight &&
            titleFontSize == defaultTitleFontSize &&
            headlineFontId == defaultHeadlineFontId &&
            bodyFontId == defaultBodyFontId
        return Button {
            updateFontSizeWithJS(defaultFontSize)
            updateLineHeightWithJS(defaultLineHeight)
            updateTitleFontSizeWithJS(defaultTitleFontSize)
            updateFontFamilyWithJS(bodyFontId: defaultBodyFontId, headlineFontId: defaultHeadlineFontId)
            actions.onFontSizeChange(defaultFontSize)
            actions.onLineHeightChange(defaultLineHeight)
            actions.onTitleFontSizeChange(defaultTitleFontSize)
            actions.onHeadlineFontChange(defaultHeadlineFontId)
            actions.onBodyFontChange(defaultBodyFontId)
        } label: {
            Label(actions.strings.resetToDefault, systemImage: "arrow.counterclockwise")
        }
        .disabled(isDisabled)
        .accessibilityIdentifier(ReaderAccessibilityIdentifiers.textSettingsResetButton)
    }

    private func textSettingSliderRow(
        title: String,
        valueLabel: String,
        valueAccessibilityIdentifier: String,
        value: Double,
        range: ClosedRange<Double>,
        minimumIcon: String,
        maximumIcon: String,
        onValueChange: @escaping (Double) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title)
                    .font(.subheadline.weight(.medium))
                Spacer()
                Text(valueLabel)
                    .font(.subheadline.weight(.semibold).monospacedDigit())
                    .foregroundStyle(.secondary)
                    .contentTransition(.numericText(value: value))
                    .accessibilityIdentifier(valueAccessibilityIdentifier)
            }

            // Rounded by hand: a stepped Slider draws a tick for every value on iOS 26.
            Slider(
                value: Binding(
                    get: { value },
                    set: { newValue in
                        let rounded = newValue.rounded()
                        if rounded != value {
                            onValueChange(rounded)
                        }
                    }
                ),
                in: range
            ) {
                Text(title)
            } minimumValueLabel: {
                Image(systemName: minimumIcon)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } maximumValueLabel: {
                Image(systemName: maximumIcon)
                    .font(.body)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
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

    private func updateTitleFontSizeWithJS(_ titleFontSize: Double) {
        guard let webContent = webContent else { return }
        webContent.evaluateJavaScript(scripts.titleFontSize(Int(titleFontSize)))
    }

    // A slow script for an uncached font must not land after a newer selection.
    private func updateFontFamilyWithJS(bodyFontId: String, headlineFontId: String) {
        fontScriptTask?.cancel()
        fontScriptTask = Task {
            let script = await scripts.fontFamily(bodyFontId, headlineFontId)
            guard !Task.isCancelled else { return }
            webContent?.evaluateJavaScript(script)
        }
    }

    private func fontFamilyTileRow(
        title: String,
        selection: String,
        accessibilityIdentifier: String,
        onSelectionChange: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 4)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(actions.fontOptions) { option in
                        fontFamilyTile(
                            option: option,
                            isSelected: option.id == selection,
                            accessibilityIdentifier: "\(accessibilityIdentifier)_\(option.id)",
                            onSelect: { onSelectionChange(option.id) }
                        )
                    }
                }
                .padding(.horizontal, 20)
            }
            .scrollClipDisabled()
            .padding(.horizontal, -20)

            if let note = actions.fontOptions.first(where: { $0.id == selection })?.note {
                Text(note)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 4)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(accessibilityIdentifier)
    }

    private func fontFamilyTile(
        option: ReaderFontOption,
        isSelected: Bool,
        accessibilityIdentifier: String,
        onSelect: @escaping () -> Void
    ) -> some View {
        Button(action: onSelect) {
            VStack(spacing: 6) {
                Text(verbatim: "Aa")
                    .font(option.font(size: 28, weight: 400))
                    .frame(height: 36)
                Text(option.label)
                    .font(.caption2.weight(isSelected ? .semibold : .regular))
                    .multilineTextAlignment(.center)
                    .lineLimit(2, reservesSpace: true)
            }
            .foregroundStyle(isSelected ? Color.accentColor : Color.primary)
            .frame(width: 92, height: 92)
            .background(
                isSelected ? Color.accentColor.opacity(0.12) : Color(.tertiarySystemBackground),
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(isSelected ? Color.accentColor : Color.clear, lineWidth: 2)
            )
            .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
        .animation(.snappy(duration: 0.2), value: isSelected)
        .accessibilityLabel(option.label)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityIdentifier(accessibilityIdentifier)
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
        headlineFontId: String,
        bodyFontId: String,
        defaultHeadlineFontId: String,
        defaultBodyFontId: String,
        titleFontSize: Double = 32,
        defaultTitleFontSize: Double = 32,
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
            headlineFontId: headlineFontId,
            bodyFontId: bodyFontId,
            defaultHeadlineFontId: defaultHeadlineFontId,
            defaultBodyFontId: defaultBodyFontId,
            titleFontSize: titleFontSize,
            defaultTitleFontSize: defaultTitleFontSize,
            showFontSizeMenu: showFontSizeMenu,
            openInBrowser: openInBrowser,
            isBottomAccessoryVisible: false,
            bottomAccessory: { EmptyView() }
        )
    }
}
