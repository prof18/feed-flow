//
//  SettingsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import FirebaseCrashlytics
import SwiftUI

struct SettingsScreen: View {
    @Environment(AppState.self)
    private var appState
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(\.dismiss)
    private var dismiss
    @Environment(\.openURL)
    private var openURL

    @StateObject private var vmStoreOwner = VMStoreOwner<SettingsViewModel>(Deps.shared.getSettingsViewModel())

    @State private var isMarkReadWhenScrollingEnabled = true
    @State private var isShowReadItemEnabled = false
    @State private var isReaderModeEnabled = false
    @State private var isSaveReaderModeContentEnabled = false
    @State private var isPrefetchArticleContentEnabled = false
    @State private var isRemoveTitleFromDescriptionEnabled = false
    @State private var isHideDescriptionEnabled = false
    @State private var isHideImagesEnabled = false
    @State private var autoDeletePeriod: AutoDeletePeriod = .disabled
    @State private var isCrashReportingEnabled = true
    @State private var leftSwipeActionType: SwipeActionType = .none
    @State private var rightSwipeActionType: SwipeActionType = .none
    @State private var dateFormat: DateFormat = .normal
    @State private var feedOrder: FeedOrder = .newestFirst
    @State private var feedLayout: FeedLayout = .list
    @State private var themeMode: ThemeMode = .system
    @State private var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()
    @State private var scaleFactor = 0.0

    @State private var imageUrl: String? = "https://lipsum.app/200x200"
    @State private var articleDescription: String? = feedFlowStrings
        .settingsFontScaleSubtitleExample

    let fetchFeeds: () -> Void

    var body: some View {
        @Bindable var appState = appState

        settingsContent
            .snackbar(messageQueue: $appState.snackbarQueue)
            .task {
                for await state in vmStoreOwner.instance.settingsState {
                    isMarkReadWhenScrollingEnabled = state.isMarkReadWhenScrollingEnabled
                    isShowReadItemEnabled = state.isShowReadItemsEnabled
                    isReaderModeEnabled = state.isReaderModeEnabled
                    isSaveReaderModeContentEnabled = state.isSaveReaderModeContentEnabled
                    isPrefetchArticleContentEnabled = state.isPrefetchArticleContentEnabled
                    isRemoveTitleFromDescriptionEnabled = state.isRemoveTitleFromDescriptionEnabled
                    isHideDescriptionEnabled = state.isHideDescriptionEnabled
                    isHideImagesEnabled = state.isHideImagesEnabled
                    autoDeletePeriod = state.autoDeletePeriod
                    isCrashReportingEnabled = state.isCrashReportingEnabled
                    leftSwipeActionType = state.leftSwipeActionType
                    rightSwipeActionType = state.rightSwipeActionType
                    dateFormat = state.dateFormat
                    feedOrder = state.feedOrder
                    feedLayout = state.feedLayout
                    themeMode = state.themeMode
                }
            }
            .task {
                for await state in vmStoreOwner.instance.feedFontSizeState {
                    self.feedFontSizes = state
                    self.scaleFactor = Double(state.scaleFactor)
                }
            }
            .onChange(of: isMarkReadWhenScrollingEnabled) {
                vmStoreOwner.instance.updateMarkReadWhenScrolling(
                    value: isMarkReadWhenScrollingEnabled)
            }
            .onChange(of: isShowReadItemEnabled) {
                vmStoreOwner.instance.updateShowReadItemsOnTimeline(value: isShowReadItemEnabled)
            }
            .onChange(of: isReaderModeEnabled) {
                vmStoreOwner.instance.updateReaderMode(value: isReaderModeEnabled)
            }
            .onChange(of: isSaveReaderModeContentEnabled) {
                vmStoreOwner.instance.updateSaveReaderModeContent(value: isSaveReaderModeContentEnabled)
            }
            .onChange(of: isPrefetchArticleContentEnabled) {
                vmStoreOwner.instance.updatePrefetchArticleContent(value: isPrefetchArticleContentEnabled)
            }
            .onChange(of: isRemoveTitleFromDescriptionEnabled) {
                vmStoreOwner.instance.updateRemoveTitleFromDescription(
                    value: isRemoveTitleFromDescriptionEnabled)
            }
            .onChange(of: isHideDescriptionEnabled) {
                vmStoreOwner.instance.updateHideDescription(value: isHideDescriptionEnabled)
                if isHideDescriptionEnabled {
                    articleDescription = nil
                } else {
                    articleDescription = feedFlowStrings.settingsFontScaleSubtitleExample
                }
            }
            .onChange(of: isHideImagesEnabled) {
                vmStoreOwner.instance.updateHideImages(value: isHideImagesEnabled)
                if isHideImagesEnabled {
                    imageUrl = nil
                } else {
                    imageUrl = "https://lipsum.app/200x200"
                }
            }
            .onChange(of: autoDeletePeriod) {
                vmStoreOwner.instance.updateAutoDeletePeriod(period: autoDeletePeriod)
            }
            .onChange(of: isCrashReportingEnabled) {
                vmStoreOwner.instance.updateCrashReporting(value: isCrashReportingEnabled)
                #if !DEBUG
                    Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(
                        isCrashReportingEnabled)
                #endif
            }
            .onChange(of: leftSwipeActionType) {
                vmStoreOwner.instance.updateSwipeAction(
                    direction: .left, action: leftSwipeActionType
                )
            }
            .onChange(of: rightSwipeActionType) {
                vmStoreOwner.instance.updateSwipeAction(
                    direction: .right, action: rightSwipeActionType
                )
            }
            .onChange(of: dateFormat) {
                vmStoreOwner.instance.updateDateFormat(format: dateFormat)
            }
            .onChange(of: feedOrder) {
                vmStoreOwner.instance.updateFeedOrder(feedOrder: feedOrder)
            }
            .onChange(of: feedLayout) {
                vmStoreOwner.instance.updateFeedLayout(feedLayout: feedLayout)
            }
            .onChange(of: themeMode) {
                vmStoreOwner.instance.updateThemeMode(mode: themeMode)
                withAnimation(.easeInOut(duration: 0.3)) {
                    appState.updateTheme(themeMode)
                }
            }
    }

    private var settingsContent: some View {
        NavigationStack {
            Form {
                FeedSection(dismiss: dismiss, appState: appState, fetchFeeds: fetchFeeds)
                BehaviourSection(
                    browserSelector: browserSelector,
                    autoDeletePeriod: $autoDeletePeriod,
                    isReaderModeEnabled: $isReaderModeEnabled,
                    isSaveReaderModeContentEnabled: $isSaveReaderModeContentEnabled,
                    isPrefetchArticleContentEnabled: $isPrefetchArticleContentEnabled,
                    feedOrder: $feedOrder,
                    themeMode: $themeMode,
                    isMarkReadWhenScrollingEnabled: $isMarkReadWhenScrollingEnabled,
                    isShowReadItemEnabled: $isShowReadItemEnabled
                )
                FeedFontSection(
                    feedFontSizes: feedFontSizes,
                    imageUrl: imageUrl,
                    articleDescription: articleDescription,
                    scaleFactor: $scaleFactor,
                    isHideDescriptionEnabled: $isHideDescriptionEnabled,
                    isHideImagesEnabled: $isHideImagesEnabled,
                    isRemoveTitleFromDescriptionEnabled: $isRemoveTitleFromDescriptionEnabled,
                    leftSwipeAction: $leftSwipeActionType,
                    rightSwipeAction: $rightSwipeActionType,
                    dateFormat: $dateFormat,
                    feedOrder: $feedOrder,
                    feedLayout: $feedLayout
                ) { newValue in
                    vmStoreOwner.instance.updateFontScale(value: Int32(newValue))
                }
                AppSection(openURL: openURL, isCrashReportingEnabled: $isCrashReportingEnabled)
            }
            .scrollContentBackground(.hidden)
            .toolbar {
                Button {
                    dismiss()
                } label: {
                    Text(feedFlowStrings.actionDone).bold()
                }
            }
            .navigationTitle(Text(feedFlowStrings.settingsTitle))
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.secondaryBackgroundColor)
        }
    }
}

private struct FeedSection: View {
    let dismiss: DismissAction
    let appState: AppState
    let fetchFeeds: () -> Void

    var body: some View {
        Section(feedFlowStrings.settingsTitleFeed) {
            NavigationLink(destination: FeedSourceListScreen()) {
                Label(feedFlowStrings.feedsTitle, systemImage: "list.bullet.rectangle.portrait")
            }

            NavigationLink(destination: AddFeedScreen()) {
                Label(feedFlowStrings.addFeed, systemImage: "plus.app")
            }

            NavigationLink(destination: ImportExportScreen(fetchFeeds: fetchFeeds)) {
                Label(feedFlowStrings.importExportOpml, systemImage: "arrow.up.arrow.down")
            }

            Button {
                dismiss()
                appState.navigate(route: CommonViewRoute.accounts)
            } label: {
                Label(feedFlowStrings.settingsAccounts, systemImage: "arrow.triangle.2.circlepath")
            }

            NavigationLink(destination: BlockedWordsScreen()) {
                Label(feedFlowStrings.settingsBlockedWords, systemImage: "exclamationmark.triangle")
            }
        }
    }
}

private struct BehaviourSection: View {
    @Bindable var browserSelector: BrowserSelector
    @Binding var autoDeletePeriod: AutoDeletePeriod
    @Binding var isReaderModeEnabled: Bool
    @Binding var isSaveReaderModeContentEnabled: Bool
    @Binding var isPrefetchArticleContentEnabled: Bool
    @Binding var feedOrder: FeedOrder
    @Binding var themeMode: ThemeMode

    @Binding var isMarkReadWhenScrollingEnabled: Bool
    @Binding var isShowReadItemEnabled: Bool

    @State private var showPrefetchWarning = false

    var body: some View {
        Section(feedFlowStrings.settingsBehaviourTitle) {
            Picker(selection: $themeMode) {
                Text(feedFlowStrings.settingsThemeSystem)
                    .tag(ThemeMode.system)
                Text(feedFlowStrings.settingsThemeLight)
                    .tag(ThemeMode.light)
                Text(feedFlowStrings.settingsThemeDark)
                    .tag(ThemeMode.dark)
            } label: {
                Label(feedFlowStrings.settingsTheme, systemImage: "moon")
            }
            
            Picker(
                selection: $browserSelector.selectedBrowser,
                content: {
                    ForEach(browserSelector.browsers, id: \.self) { period in
                        Text(period.name).tag(period as Browser?)
                    }
                },
                label: {
                    Label(feedFlowStrings.browserSelectionButton, systemImage: "globe")
                }
            )

            Picker(selection: $autoDeletePeriod) {
                Text(feedFlowStrings.settingsAutoDeletePeriodDisabled)
                    .tag(AutoDeletePeriod.disabled)
                Text(feedFlowStrings.settingsAutoDeletePeriodOneWeek)
                    .tag(AutoDeletePeriod.oneWeek)
                Text(feedFlowStrings.settingsAutoDeletePeriodTwoWeeks)
                    .tag(AutoDeletePeriod.twoWeeks)
                Text(feedFlowStrings.settingsAutoDeletePeriodOneMonth)
                    .tag(AutoDeletePeriod.oneMonth)
            } label: {
                Label(feedFlowStrings.settingsAutoDelete, systemImage: "arrow.3.trianglepath")
            }

            Toggle(isOn: $isReaderModeEnabled) {
                Label(feedFlowStrings.settingsReaderMode, systemImage: "doc.text")
            }.onTapGesture {
                isReaderModeEnabled.toggle()
            }

            Toggle(isOn: $isSaveReaderModeContentEnabled) {
                Label(feedFlowStrings.settingsSaveReaderModeContent, systemImage: "doc.text.fill")
            }.onTapGesture {
                isSaveReaderModeContentEnabled.toggle()
            }

            Toggle(isOn: $isPrefetchArticleContentEnabled) {
                Label(feedFlowStrings.settingsPrefetchArticleContent, systemImage: "cloud.fill")
            }
            .onChange(of: isPrefetchArticleContentEnabled) { _, newValue in
                if newValue {
                    showPrefetchWarning = true
                }
            }
            .alert(feedFlowStrings.settingsPrefetchArticleContent, isPresented: $showPrefetchWarning) {
                Button(feedFlowStrings.cancelButton, role: .cancel) {
                    isPrefetchArticleContentEnabled = false
                }
                Button(feedFlowStrings.confirmButton) {
                    // Keep enabled
                }
            } message: {
                Text(feedFlowStrings.settingsPrefetchArticleContentWarning)
            }

            Toggle(isOn: $isMarkReadWhenScrollingEnabled) {
                Label(feedFlowStrings.toggleMarkReadWhenScrolling, systemImage: "scroll")
            }.onTapGesture {
                isMarkReadWhenScrollingEnabled.toggle()
            }

            Toggle(isOn: $isShowReadItemEnabled) {
                Label(feedFlowStrings.settingsToggleShowReadArticles, systemImage: "eye")
            }.onTapGesture {
                isShowReadItemEnabled.toggle()
            }
        }
    }
}

private struct AppSection: View {
    let openURL: OpenURLAction
    @Binding var isCrashReportingEnabled: Bool

    var body: some View {
        Section(feedFlowStrings.settingsAppTitle) {
            Button(
                action: {
                    let subject = feedFlowStrings.issueContentTitle
                    let content = feedFlowStrings.issueContentTemplate

                    if let url = URL(
                        string: UserFeedbackReporter.shared.getEmailUrl(
                            subject: subject, content: content
                        )
                    ) {
                        openURL(url)
                    }
                },
                label: {
                    Label(feedFlowStrings.reportIssueButton, systemImage: "ladybug")
                }
            )

            Toggle(isOn: $isCrashReportingEnabled) {
                Label(
                    feedFlowStrings.settingsCrashReporting,
                    systemImage: "exclamationmark.bubble.fill"
                )
            }.onTapGesture {
                isCrashReportingEnabled.toggle()
            }

            NavigationLink(destination: AboutScreen()) {
                Label(feedFlowStrings.aboutButton, systemImage: "info.circle")
            }
        }
    }
}
