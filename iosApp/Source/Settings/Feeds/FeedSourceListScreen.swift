//
//  FeedListScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct FeedSourceListScreen: View {
    
    @EnvironmentObject var appState: AppState
    @StateObject var feedSourceViewModel: FeedSourceListViewModel = KotlinDependencies.shared.getFeedSourceListViewModel()
    
    @State var feedState: [FeedSource] = []
    
    var body: some View {
        FeedSourceListContent(
            feedState: $feedState,
            deleteFeedSource: { feedSource in
                feedSourceViewModel.deleteFeedSource(feedSource: feedSource)
            }
        )
        .task {
            do {
                let stream = asyncSequence(for: feedSourceViewModel.feedSourcesStateFlow)
                for try await state in stream {
                    self.feedState = state
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

private struct FeedSourceListContent: View {
    
    @Environment(\.presentationMode) var presentationMode
    
    @State private var showAddFeed = false
    @Binding var feedState: [FeedSource]
    
    let deleteFeedSource: (FeedSource) -> Void

    
    var body: some View {
        NavigationStack {
            VStack {
                if (feedState.isEmpty) {
                    VStack {
                        Text(MR.strings().no_feeds_found_message.localized)
                            .font(.body)
                        
                        NavigationLink(value: SheetPage.addFeed) {
                            Text(MR.strings().add_feed.localized)
                        }
                    }
                } else {
                    List {
                        ForEach(feedState, id: \.self.id) { feedSource in
                            
                            VStack(alignment: .leading) {
                                Text(feedSource.title)
                                    .font(.system(size: 16))
                                    .padding(.top, Spacing.xsmall)
                                
                                
                                Text(feedSource.url)
                                    .font(.system(size: 14))
                                    .padding(.top, Spacing.xxsmall)
                                    .padding(.bottom, Spacing.xsmall)
                                
                            }
                            .id(feedSource.id)
                            .contextMenu {
                                Button {
                                    deleteFeedSource(feedSource)
                                } label: {
                                    Label(
                                        MR.strings().delete_feed.localized,
                                        systemImage: "trash"
                                    )
                                }
                            }
                            .onTapGesture {
                                // TODO: edit feed
                            }
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationDestination(for: SheetPage.self) { page in
                switch page {
                case .addFeed:
                    AddFeedScreen()
                }
            }
            .toolbar {
                
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(
                        action: {
                            self.presentationMode.wrappedValue.dismiss()
                        }
                    ) {
                        Image(systemName: "xmark")
                    }
                }
                
                ToolbarItem(placement: .navigationBarLeading) {
                    Text(MR.strings().feeds_title.localized)
                        .font(.title2)
                        .padding(.vertical, Spacing.medium)
                }
                
                ToolbarItem(placement: .primaryAction) {
                    NavigationLink(value: SheetPage.addFeed) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showAddFeed) {
                AddFeedScreen()
            }
        }
    }
}

struct FeedSourceListContent_Previews: PreviewProvider {
    static var previews: some View {
        FeedSourceListContent(
            feedState: .constant(PreviewItemsKt.feedSourcesForPreview),
            deleteFeedSource: { _ in }
        )
    }
}


struct FeedSourceListContentEmpty_Previews: PreviewProvider {
    static var previews: some View {
        FeedSourceListContent(
            feedState: .constant([]),
            deleteFeedSource: { _ in }
        )
    }
}
