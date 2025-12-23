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

    @State private var settingsState = SettingsState(
        isMarkReadWhenScrollingEnabled: true,
        isShowReadItemsEnabled: false,
        isReaderModeEnabled: false,
        isSaveReaderModeContentEnabled: false,
        isPrefetchArticleContentEnabled: false,
        isExperimentalParsingEnabled: false,
        isRemoveTitleFromDescriptionEnabled: false,
        isHideDescriptionEnabled: false,
        isHideImagesEnabled: false,
        isHideDateEnabled: false,
        autoDeletePeriod: .disabled,
        isCrashReportingEnabled: true,
        syncPeriod: .oneHour,
        leftSwipeActionType: .none,
        rightSwipeActionType: .none,
        dateFormat: .normal,
        timeFormat: .hours24,
        feedOrder: .newestFirst,
        feedLayout: .list,
        themeMode: .system
    )
    @State private var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()
    @State private var scaleFactor = 0.0

    @State private var imageUrl: String? = "https://lipsum.app/200x200"
    @State private var articleDescription: String? = feedFlowStrings
        .settingsFontScaleSubtitleExample

    let fetchFeeds: () -> Void

    var body: some View {
        @Bindable var appState = appState
        let viewWithTasks = settingsContent
            .snackbar(messageQueue: $appState.snackbarQueue)
            .task {
                for await state in vmStoreOwner.instance.settingsState {
                    self.settingsState = state
                }
            }
            .task {
                for await state in vmStoreOwner.instance.feedFontSizeState {
                    self.feedFontSizes = state
                    self.scaleFactor = Double(state.scaleFactor)
                }
            }

        return viewWithTasks
    }

    private var settingsContent: some View {
        NavigationStack {
            Form {
                FeedSection(dismiss: dismiss, appState: appState, fetchFeeds: fetchFeeds)
                BehaviourSection(
                    browserSelector: browserSelector,
                    autoDeletePeriod: Binding(
                        get: { settingsState.autoDeletePeriod },
                        set: { vmStoreOwner.instance.updateAutoDeletePeriod(period: $0) }
                    ),
                    isReaderModeEnabled: Binding(
                        get: { settingsState.isReaderModeEnabled },
                        set: { vmStoreOwner.instance.updateReaderMode(value: $0) }
                    ),
                    isSaveReaderModeContentEnabled: Binding(
                        get: { settingsState.isSaveReaderModeContentEnabled },
                        set: { vmStoreOwner.instance.updateSaveReaderModeContent(value: $0) }
                    ),
                    isPrefetchArticleContentEnabled: Binding(
                        get: { settingsState.isPrefetchArticleContentEnabled },
                        set: { vmStoreOwner.instance.updatePrefetchArticleContent(value: $0) }
                    ),
                    feedOrder: Binding(
                        get: { settingsState.feedOrder },
                        set: { vmStoreOwner.instance.updateFeedOrder(feedOrder: $0) }
                    ),
                    themeMode: Binding(
                        get: { settingsState.themeMode },
                        set: { newValue in
                            vmStoreOwner.instance.updateThemeMode(mode: newValue)
                            withAnimation(.easeInOut(duration: 0.3)) {
                                appState.updateTheme(newValue)
                            }
                        }
                    ),
                    isMarkReadWhenScrollingEnabled: Binding(
                        get: { settingsState.isMarkReadWhenScrollingEnabled },
                        set: { vmStoreOwner.instance.updateMarkReadWhenScrolling(value: $0) }
                    ),
                    isShowReadItemEnabled: Binding(
                        get: { settingsState.isShowReadItemsEnabled },
                        set: { vmStoreOwner.instance.updateShowReadItemsOnTimeline(value: $0) }
                    )
                )
                FeedFontSection(
                    feedFontSizes: feedFontSizes,
                    imageUrl: imageUrl,
                    articleDescription: articleDescription,
                    scaleFactor: $scaleFactor,
                    isHideDescriptionEnabled: Binding(
                        get: { settingsState.isHideDescriptionEnabled },
                        set: { newValue in
                            vmStoreOwner.instance.updateHideDescription(value: newValue)
                            if newValue {
                                articleDescription = nil
                            } else {
                                articleDescription = feedFlowStrings.settingsFontScaleSubtitleExample
                            }
                        }
                    ),
                    isHideImagesEnabled: Binding(
                        get: { settingsState.isHideImagesEnabled },
                        set: { newValue in
                            vmStoreOwner.instance.updateHideImages(value: newValue)
                            if newValue {
                                imageUrl = nil
                            } else {
                                imageUrl = "https://lipsum.app/200x200"
                            }
                        }
                    ),
                    isHideDateEnabled: Binding(
                        get: { settingsState.isHideDateEnabled },
                        set: { vmStoreOwner.instance.updateHideDate(value: $0) }
                    ),
                    isRemoveTitleFromDescriptionEnabled: Binding(
                        get: { settingsState.isRemoveTitleFromDescriptionEnabled },
                        set: { vmStoreOwner.instance.updateRemoveTitleFromDescription(value: $0) }
                    ),
                    leftSwipeAction: Binding(
                        get: { settingsState.leftSwipeActionType },
                        set: { vmStoreOwner.instance.updateSwipeAction(direction: .left, action: $0) }
                    ),
                    rightSwipeAction: Binding(
                        get: { settingsState.rightSwipeActionType },
                        set: { vmStoreOwner.instance.updateSwipeAction(direction: .right, action: $0) }
                    ),
                    dateFormat: Binding(
                        get: { settingsState.dateFormat },
                        set: { vmStoreOwner.instance.updateDateFormat(format: $0) }
                    ),
                    timeFormat: Binding(
                        get: { settingsState.timeFormat },
                        set: { vmStoreOwner.instance.updateTimeFormat(format: $0) }
                    ),
                    feedOrder: Binding(
                        get: { settingsState.feedOrder },
                        set: { vmStoreOwner.instance.updateFeedOrder(feedOrder: $0) }
                    ),
                    feedLayout: Binding(
                        get: { settingsState.feedLayout },
                        set: { vmStoreOwner.instance.updateFeedLayout(feedLayout: $0) }
                    )
                ) { newValue in
                    vmStoreOwner.instance.updateFontScale(value: Int32(newValue))
                }
                DangerSection {
                    vmStoreOwner.instance.clearDownloadedArticleContent()
                }
                AppSection(
                    openURL: openURL,
                    isCrashReportingEnabled: Binding(
                        get: { settingsState.isCrashReportingEnabled },
                        set: { newValue in
                            vmStoreOwner.instance.updateCrashReporting(value: newValue)
                            #if !DEBUG
                                Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(newValue)
                            #endif
                        }
                    ),
                    appState: appState,
                    dismiss: dismiss
                )
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
                Text(feedFlowStrings.settingsAutoDeletePeriodOneDay)
                    .tag(AutoDeletePeriod.oneDay)
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

            Toggle(isOn: Binding(
                get: { isPrefetchArticleContentEnabled },
                set: { newValue in
                    if newValue {
                        showPrefetchWarning = true
                    } else {
                        isPrefetchArticleContentEnabled = false
                    }
                }
            )) {
                Label(feedFlowStrings.settingsPrefetchArticleContent, systemImage: "cloud.fill")
            }
            .alert(feedFlowStrings.settingsPrefetchArticleContent, isPresented: $showPrefetchWarning) {
                Button(feedFlowStrings.cancelButton, role: .cancel) { }
                Button(feedFlowStrings.confirmButton) {
                    isPrefetchArticleContentEnabled = true
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
    let appState: AppState
    let dismiss: DismissAction

    var body: some View {
        Section(feedFlowStrings.settingsAppTitle) {
            Button(
                action: {
                    let subject = feedFlowStrings.issueContentTitle
                    let content = feedFlowStrings.issueContentTemplate

                    if let url = URL(
                        string: Deps.shared.getUserFeedbackReporter().getEmailUrl(
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

// TODO: Enabled FAQ button when faqs are ready on the website
//             Button {
//                 let languageCode = Locale.current.language.languageCode?.identifier ?? "en"
//                 let faqUrl = "https://feedflow.dev/\(languageCode)/faq"
//
//                 if let url = URL(string: faqUrl) {
//                     dismiss()
//                     appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
//                 }
//             } label: {
//                 Label(feedFlowStrings.aboutMenuFaq, systemImage: "questionmark")
//             }

            NavigationLink(destination: AboutScreen()) {
                Label(feedFlowStrings.aboutButton, systemImage: "info.circle")
            }
        }
    }
}

private struct DangerSection: View {
    let onClearDownloadedArticles: () -> Void

    @State private var showClearDialog = false

    var body: some View {
        Section(header: Text(feedFlowStrings.settingsDangerTitle)
            .foregroundColor(.red.opacity(0.8))) {
            Button {
                showClearDialog = true
            } label: {
                Label(feedFlowStrings.settingsClearDownloadedArticles, systemImage: "trash")
            }
            .alert(
                feedFlowStrings.settingsClearDownloadedArticlesDialogTitle,
                isPresented: $showClearDialog
            ) {
                Button(feedFlowStrings.cancelButton, role: .cancel) { }
                Button(feedFlowStrings.confirmButton, role: .destructive) {
                    onClearDownloadedArticles()
                }
            } message: {
                Text(feedFlowStrings.settingsClearDownloadedArticlesDialogMessage)
            }
        }
    }
}

private extension View {
    @ViewBuilder
    func applying<T: View>(_ transform: (Self) -> T) -> T {
        transform(self)
    }
}
