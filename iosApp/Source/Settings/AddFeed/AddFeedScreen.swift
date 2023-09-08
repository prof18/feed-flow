//
//  SwiftUIView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct AddFeedScreen: View {

    @EnvironmentObject var appState: AppState
    @Environment(\.presentationMode) var presentationMode

    @State private var feedName = ""
    @State private var feedURL = ""
    @State private var isInvalidUrl = false

    @StateObject var addFeedViewModel: AddFeedViewModel = KotlinDependencies.shared.getAddFeedViewModel()

    var body: some View {
        VStack(alignment: .leading) {
            TextField(
                localizer.feed_name.localized,
                text: $feedName
            )
            .textFieldStyle(RoundedBorderTextFieldStyle())
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.regular)

            TextField(
                localizer.feed_url.localized,
                text: $feedURL
            )
            .keyboardType(.webSearch)
            .border(isInvalidUrl ? Color.red : Color.clear)
            .textFieldStyle(RoundedBorderTextFieldStyle())
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.regular)

            if isInvalidUrl {
                Text(localizer.invalid_rss_url.localized)
                    .padding(.horizontal, Spacing.regular)
                    .frame(alignment: .leading)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            HStack {
                Spacer()

                Button(
                    action: {
                        addFeedViewModel.addFeed()
                    },
                    label: {
                        Text(localizer.add_feed.localized)
                    }
                )
                .frame(alignment: .center)
                .buttonStyle(.bordered)
                .padding(.top, Spacing.regular)

                Spacer()
            }

            Spacer()
        }
        .navigationTitle(localizer.add_feed.localized)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: feedName) { value in
            addFeedViewModel.updateFeedNameTextFieldValue(feedNameTextFieldValue: value)
        }
        .onChange(of: feedURL) { value in
            addFeedViewModel.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: value)
        }
        .task {
            do {
                let stream = asyncSequence(for: addFeedViewModel.isAddDoneStateFlow)
                for try await isAddDone in stream where isAddDone as? Bool ?? false {
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: localizer.feed_added_message.localized,
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                    self.addFeedViewModel.clearAddDoneState()
                    presentationMode.wrappedValue.dismiss()
                }
            } catch {
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: localizer.generic_error_message.localized,
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: addFeedViewModel.isInvalidRssFeedFlow)
                for try await isInvalidRss in stream {
                    self.isInvalidUrl = isInvalidRss as? Bool ?? false
                }
            } catch {
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: localizer.generic_error_message.localized,
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
        }
    }
}

struct SwiftUIView_Previews: PreviewProvider {
    static var previews: some View {
        AddFeedScreen()
    }
}
