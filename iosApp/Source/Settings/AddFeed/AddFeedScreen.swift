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

    @State private var feedURL = ""
    @State private var showError = false
    @State private var errorMessage = ""

    @StateObject var addFeedViewModel: AddFeedViewModel = KotlinDependencies.shared.getAddFeedViewModel()

    var body: some View {
        VStack(alignment: .leading) {
            TextField(
                localizer.feed_url.localized,
                text: $feedURL
            )
            .keyboardType(.webSearch)
            .border(showError ? Color.red : Color.clear)
            .textFieldStyle(RoundedBorderTextFieldStyle())
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.regular)

            if showError {
                Text(errorMessage)
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
        .onChange(of: feedURL) { value in
            addFeedViewModel.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: value)
        }
        .task {
            do {
                let stream = asyncSequence(for: addFeedViewModel.feedAddedState)
                for try await state in stream {
                    switch state {
                    case let addedState as FeedAddedState.FeedAdded:
                        self.addFeedViewModel.clearAddDoneState()
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: addedState.message.localized(),
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                        presentationMode.wrappedValue.dismiss()

                    case is FeedAddedState.FeedNotAdded:
                        errorMessage = ""
                        showError = false

                    case let errorState as FeedAddedState.Error:
                        errorMessage = errorState.errorMessage.localized()
                        showError = true

                    default:
                        break
                    }
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
