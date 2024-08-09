//
//  SettingsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/01/24.
//  Copyright © 2024. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct SettingsScreen: View {

    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var browserSelector: BrowserSelector

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    @StateObject
    private var vmStoreOwner = VMStoreOwner<SettingsViewModel>(Deps.shared.getSettingsViewModel())

    @State private var isMarkReadWhenScrollingEnabled = true
    @State private var isShowReadItemEnabled = false
    @State private var isReaderModeEnabled = false
    @State private var isRemoveTitleFromDescriptionEnabled = false

    var body: some View {
        settingsContent
            .task {
                for await state in vmStoreOwner.instance.settingsState {
                    self.isMarkReadWhenScrollingEnabled = state.isMarkReadWhenScrollingEnabled
                    self.isShowReadItemEnabled = state.isShowReadItemsEnabled
                    self.isReaderModeEnabled = state.isReaderModeEnabled
                    self.isRemoveTitleFromDescriptionEnabled = state.isRemoveTitleFromDescriptionEnabled
                }
            }
            .onChange(of: isMarkReadWhenScrollingEnabled) { newValue in
                vmStoreOwner.instance.updateMarkReadWhenScrolling(value: newValue)
            }
            .onChange(of: isShowReadItemEnabled) { newValue in
                vmStoreOwner.instance.updateShowReadItemsOnTimeline(value: newValue)
            }.onChange(of: isReaderModeEnabled) { newValue in
                vmStoreOwner.instance.updateReaderMode(value: newValue)
            }.onChange(of: isRemoveTitleFromDescriptionEnabled) { newValue in
                vmStoreOwner.instance.updateRemoveTitleFromDescription(value: newValue)
            }
    }

    private var settingsContent: some View {
        NavigationStack {
            Form {
                feedSection
                behaviourSection
                appSection
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
            .navigationTitle(
                Text(feedFlowStrings.settingsTitle)
            )
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.secondaryBackgroundColor)
        }
    }

    @ViewBuilder
    private var feedSection: some View {
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
                self.dismiss()
                appState.navigate(route: CommonViewRoute.accounts)
            } label: {
                Label(feedFlowStrings.settingsAccounts, systemImage: "arrow.triangle.2.circlepath")
            }
        }
    }

    @ViewBuilder
    private var behaviourSection: some View {
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
            .hoverEffect()
            .accessibilityIdentifier(TestingTag.shared.BROWSER_SELECTOR)

            Toggle(isOn: $isReaderModeEnabled) {
                Label(feedFlowStrings.settingsReaderMode, systemImage: "newspaper")
            }.onTapGesture {
                isReaderModeEnabled.toggle()
            }

            Toggle(isOn: $isMarkReadWhenScrollingEnabled) {
                Label(feedFlowStrings.toggleMarkReadWhenScrolling, systemImage: "envelope.open")
            }.onTapGesture {
                isMarkReadWhenScrollingEnabled.toggle()
            }
            .accessibilityIdentifier(TestingTag.shared.MARK_AS_READ_SCROLLING_SWITCH)

            Toggle(isOn: $isShowReadItemEnabled) {
                Label(feedFlowStrings.settingsToggleShowReadArticles, systemImage: "text.badge.checkmark")
            }.onTapGesture {
                isShowReadItemEnabled.toggle()
            }

            Toggle(isOn: $isRemoveTitleFromDescriptionEnabled) {
                Label(feedFlowStrings.settingsHideDuplicatedTitleFromDesc, systemImage: "eye.slash")
            }.onTapGesture {
                isRemoveTitleFromDescriptionEnabled.toggle()
            }
        }
    }

    @ViewBuilder
    private var appSection: some View {
        Section(feedFlowStrings.settingsAppTitle) {
            Button(
                action: {
                    let subject = feedFlowStrings.issueContentTitle
                    let content = feedFlowStrings.issueContentTemplate

                    if let url = URL(
                        string: UserFeedbackReporter.shared.getEmailUrl(subject: subject, content: content)
                    ) {
                        self.openURL(url)
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

#Preview {
    SettingsScreen()
        .environmentObject(BrowserSelector())
}
