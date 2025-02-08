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
  @State private var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()
  @State private var scaleFactor = 0.0

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
      }.onChange(of: isReaderModeEnabled) {
        vmStoreOwner.instance.updateReaderMode(value: isReaderModeEnabled)
      }.onChange(of: isRemoveTitleFromDescriptionEnabled) {
        vmStoreOwner.instance.updateRemoveTitleFromDescription(
          value: isRemoveTitleFromDescriptionEnabled)
      }
  }

  private var settingsContent: some View {
    NavigationStack {
      Form {
        feedSection
        behaviourSection
        feedFontSizesSection
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
      @Bindable var browserSelector = browserSelector
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
  private var feedFontSizesSection: some View {
    Section(feedFlowStrings.settingsFeedListTitle) {
      VStack(alignment: .leading) {
        VStack {
          FeedItemView(
            feedItem: FeedItem(
              id: "1",
              url: "https://www.example.com",
              title: feedFlowStrings.settingsFontScaleTitleExample,
              subtitle: feedFlowStrings.settingsFontScaleSubtitleExample,
              content: nil,
              imageUrl: "https://lipsum.app/200x200",
              feedSource: FeedSource(
                id: "1",
                url: "https://www.example.it",
                title: feedFlowStrings.settingsFontScaleFeedSourceExample,
                category: nil,
                lastSyncTimestamp: nil,
                logoUrl: nil,
                linkOpeningPreference: .default
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
                vmStoreOwner.instance.updateFontScale(value: Int32(newValue))
              }
            ),
            in: -4...16,
            step: 1.0
          )

          Image(systemName: "plus")
        }
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
    .environment(BrowserSelector())
}
