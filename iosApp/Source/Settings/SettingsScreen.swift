//
//  SettingsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct SettingsScreen: View {
    @Environment(AppState.self) private var appState
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    @StateObject
    private var vmStoreOwner = VMStoreOwner<SettingsViewModel>(Deps.shared.getSettingsViewModel())

    @State private var isMarkReadWhenScrollingEnabled = true
    @State private var isShowReadItemEnabled = false
    @State private var isReaderModeEnabled = false
    @State private var isRemoveTitleFromDescriptionEnabled = false
    @State private var isHideDescriptionEnabled = false
    @State private var isHideImagesEnabled = false
    @State private var autoDeletePeriod: AutoDeletePeriod = .disabled
    @State private var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()
    @State private var scaleFactor = 0.0

    @State private var imageUrl: String? = "https://lipsum.app/200x200"
    @State private var articleDescription: String? = feedFlowStrings.settingsFontScaleSubtitleExample

    var body: some View {
        settingsContent
            .task {
                for await state in vmStoreOwner.instance.settingsState {
                    isMarkReadWhenScrollingEnabled = state.isMarkReadWhenScrollingEnabled
                    isShowReadItemEnabled = state.isShowReadItemsEnabled
                    isReaderModeEnabled = state.isReaderModeEnabled
                    isRemoveTitleFromDescriptionEnabled = state.isRemoveTitleFromDescriptionEnabled
                    isHideDescriptionEnabled = state.isHideDescriptionEnabled
                    isHideImagesEnabled = state.isHideImagesEnabled
                    autoDeletePeriod = state.autoDeletePeriod
                }
            }
            .task {
                for await state in vmStoreOwner.instance.feedFontSizeState {
                    self.feedFontSizes = state
                    self.scaleFactor = Double(state.scaleFactor)
                }
            }
            .onChange(of: isMarkReadWhenScrollingEnabled) {
                vmStoreOwner.instance.updateMarkReadWhenScrolling(value: isMarkReadWhenScrollingEnabled)
            }
            .onChange(of: isShowReadItemEnabled) {
                vmStoreOwner.instance.updateShowReadItemsOnTimeline(value: isShowReadItemEnabled)
            }
            .onChange(of: isReaderModeEnabled) {
                vmStoreOwner.instance.updateReaderMode(value: isReaderModeEnabled)
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
    }

    private var settingsContent: some View {
        NavigationStack {
            Form {
                FeedSection(dismiss: dismiss, appState: appState)
                BehaviourSection(
                    browserSelector: browserSelector,
                    autoDeletePeriod: $autoDeletePeriod,
                    isReaderModeEnabled: $isReaderModeEnabled,
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
                    onScaleFactorChange: { newValue in
                        vmStoreOwner.instance.updateFontScale(value: Int32(newValue))
                    }
                )
                AppSection(openURL: openURL)
            }
            .scrollContentBackground(.hidden)
            .toolbar {
                Button {
                    dismiss()
                } label: {
                    Text(feedFlowStrings.actionDone).bold()
                }
                .accessibilityIdentifier(TestingTag.shared.BACK_BUTTON)
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

    var body: some View {
        Section(feedFlowStrings.settingsTitleFeed) {
            NavigationLink(destination: FeedSourceListScreen()) {
                Label(feedFlowStrings.feedsTitle, systemImage: "list.bullet.rectangle.portrait")
            }
            .accessibilityIdentifier(TestingTag.shared.SETTINGS_FEED_ITEM)

            NavigationLink(destination: AddFeedScreen()) {
                Label(feedFlowStrings.addFeed, systemImage: "plus.app")
            }

            NavigationLink(destination: ImportExportScreen()) {
                Label(feedFlowStrings.importExportOpml, systemImage: "arrow.up.arrow.down")
            }

            Button {
                dismiss()
                appState.navigate(route: CommonViewRoute.accounts)
            } label: {
                Label(feedFlowStrings.settingsAccounts, systemImage: "arrow.triangle.2.circlepath")
            }
        }
    }
}

private struct BehaviourSection: View {
    @Bindable var browserSelector: BrowserSelector
    @Binding var autoDeletePeriod: AutoDeletePeriod
    @Binding var isReaderModeEnabled: Bool
    @Binding var isMarkReadWhenScrollingEnabled: Bool
    @Binding var isShowReadItemEnabled: Bool

    var body: some View {
        Section(feedFlowStrings.settingsBehaviourTitle) {
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

private struct FeedFontSection: View {
    let feedFontSizes: FeedFontSizes
    let imageUrl: String?
    let articleDescription: String?
    @Binding var scaleFactor: Double
    @Binding var isHideDescriptionEnabled: Bool
    @Binding var isHideImagesEnabled: Bool
    @Binding var isRemoveTitleFromDescriptionEnabled: Bool
    let onScaleFactorChange: (Double) -> Void

    var body: some View {
        Section(feedFlowStrings.settingsFeedListTitle) {
            VStack(alignment: .leading) {
                VStack {
                    FeedItemView(
                        feedItem: FeedItem(
                            id: "1",
                            url: "https://www.example.com",
                            title: feedFlowStrings.settingsFontScaleTitleExample,
                            subtitle: articleDescription,
                            content: nil,
                            imageUrl: imageUrl,
                            feedSource: FeedSource(
                                id: "1",
                                url: "https://www.example.it",
                                title: feedFlowStrings.settingsFontScaleFeedSourceExample,
                                category: nil,
                                lastSyncTimestamp: nil,
                                logoUrl: nil,
                                linkOpeningPreference: .default,
                                isHiddenFromTimeline: false,
                                isPinned: false
                            ),
                            pubDateMillis: nil,
                            isRead: false,
                            dateString: "01/01",
                            commentsUrl: nil,
                            isBookmarked: false
                        ),
                        index: 0,
                        feedFontSizes: feedFontSizes
                    )
                    .background(Color.secondaryBackgroundColor)
                    .padding(Spacing.small)
                    .padding(.top, Spacing.small)
                    .cornerRadius(Spacing.small)
                    .shadow(color: Color.black.opacity(0.1), radius: 5, x: 0, y: 2)
                }

                Text(feedFlowStrings.settingsFeedListScaleTitle)
                    .padding(.top)

                // 16 default
                // 12 min ( -4 )
                // 32 max ( +16 )
                HStack {
                    Image(systemName: "minus")

                    Slider(
                        value: Binding(
                            get: { scaleFactor },
                            set: { newValue in
                                scaleFactor = newValue
                                onScaleFactorChange(newValue)
                            }
                        ),
                        in: -4 ... 16,
                        step: 1.0
                    )

                    Image(systemName: "plus")
                }
            }.padding(.bottom, Spacing.regular)

            Toggle(isOn: $isHideDescriptionEnabled) {
                Label(feedFlowStrings.settingsHideDescription, systemImage: "text.page.slash")
            }.onTapGesture {
                isHideDescriptionEnabled.toggle()
            }

            Toggle(isOn: $isHideImagesEnabled) {
                Label(feedFlowStrings.settingsHideImages, systemImage: "square.slash")
            }.onTapGesture {
                isHideImagesEnabled.toggle()
            }

            Toggle(isOn: $isRemoveTitleFromDescriptionEnabled) {
                Label(feedFlowStrings.settingsHideDuplicatedTitleFromDesc, systemImage: "eye.slash")
            }.onTapGesture {
                isRemoveTitleFromDescriptionEnabled.toggle()
            }
        }
    }
}

private struct AppSection: View {
    let openURL: OpenURLAction

    var body: some View {
        Section(feedFlowStrings.settingsAppTitle) {
            Button(
                action: {
                    let subject = feedFlowStrings.issueContentTitle
                    let content = feedFlowStrings.issueContentTemplate

                    if let url = URL(
                        string: UserFeedbackReporter.shared.getEmailUrl(subject: subject, content: content)
                    ) {
                        openURL(url)
                    }
                },
                label: {
                    Label(feedFlowStrings.reportIssueButton, systemImage: "ladybug")
                }
            )

            NavigationLink(destination: AboutScreen()) {
                Label(feedFlowStrings.aboutButton, systemImage: "info.circle")
            }
            .accessibilityIdentifier(TestingTag.shared.ABOUT_SETTINGS_ITEM)
        }
    }
}
