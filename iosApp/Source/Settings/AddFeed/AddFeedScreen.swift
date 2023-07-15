//
//  SwiftUIView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync


struct AddFeedScreen: View {
    
    @EnvironmentObject var appState: AppState
    @Environment(\.presentationMode) var presentationMode
    
    @State private var feedName = ""
    @State private var feedURL = ""
    
    @StateObject var addFeedViewModel: AddFeedViewModel = KotlinDependencies.shared.getAddFeedViewModel()
    
    var body: some View {
        VStack {
            TextField(
                MR.strings().feed_name.localized,
                text: $feedName
            )
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.top, Spacing.regular)
                .padding(.horizontal, Spacing.regular)
            
            TextField(
                MR.strings().feed_url.localized,
                text: $feedURL
            )
                .keyboardType(.webSearch)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.top, Spacing.regular)
                .padding(.horizontal, Spacing.regular)
            
            
            Button(
                action: {
                    addFeedViewModel.addFeed()
                }
            ) {
                Text(MR.strings().add_feed.localized)
            }
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
            
            Spacer()
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Text(MR.strings().add_feed.localized)
                    .font(.title2)
            }
        }
        .onChange(of: feedName) { value in
            addFeedViewModel.updateFeedNameTextFieldValue(feedNameTextFieldValue: value)
        }
        .onChange(of: feedURL) { value in
            addFeedViewModel.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: value)
        }
        .task {
            do {
                let stream = asyncSequence(for: addFeedViewModel.isAddDoneStateFlow)
                for try await isAddDone in stream {
                    if isAddDone as! Bool {
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: MR.strings().feed_added_message.localized,
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                        self.addFeedViewModel.clearAddDoneState()
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            } catch {
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: MR.strings().generic_error_message.localized,
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
