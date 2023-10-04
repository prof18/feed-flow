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
    @State private var isCategoriesSelectorExpanded = false
    @State private var headerMessage = localizer.no_category_selected_header.localized
    @State private var categoryItems: [CategoriesState.CategoryItem] = []
    @State private var newCategoryName = ""

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

            DisclosureGroup(
                isExpanded: $isCategoriesSelectorExpanded,
                content: {
                    ForEach(categoryItems, id: \.self.id) { categoryItem in
                        HStack {
                            RadioButtonView(
                                title: categoryItem.name,
                                isSelected: categoryItem.isSelected,
                                onRadioSelected: {
                                    withAnimation {
                                        isCategoriesSelectorExpanded.toggle()
                                    }
                                    categoryItem.onClick(CategoryId(value: categoryItem.id))
                                }
                            )

                            Spacer()
                        }
                    }
                    .padding(.top, Spacing.small)

                    HStack {
                        TextField(
                            localizer.new_category_hint.localized,
                            text: $newCategoryName
                        )
                        .textFieldStyle(RoundedBorderTextFieldStyle())

                        Button(
                            action: {
                                addFeedViewModel.addNewCategory(
                                    categoryName: CategoryName(name: newCategoryName)
                                )
                            },
                            label: {
                                Image(systemName: "plus")
                            }
                        )
                        .disabled(newCategoryName.isEmpty)
                    }
                    .padding(.top, Spacing.regular)
                },
                label: {
                    Text(headerMessage)
                }
            )
            .padding(Spacing.regular)

            Button(
                action: {
                    addFeedViewModel.addFeed()
                },
                label: {
                    Text(localizer.add_feed.localized)
                        .frame(maxWidth: .infinity)
                }
            )
            .disabled(feedURL.isEmpty)
            .buttonStyle(.bordered)
            .padding(.horizontal, Spacing.regular)

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
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: addFeedViewModel.categoriesState)
                for try await state in stream {
                    isCategoriesSelectorExpanded = state.isExpanded

                    if let header = state.header {
                        self.headerMessage = header
                    } else {
                        self.headerMessage = localizer.no_category_selected_header.localized
                    }

                    self.categoryItems = state.categories
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}

private struct RadioButtonView: View {
    var title: String
    var isSelected: Bool
    var onRadioSelected: () -> Void

    var body: some View {
        Button(action: onRadioSelected) {
            HStack {
                Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                Text(title)
            }
        }
        .foregroundColor(.primary)
        .padding(.vertical, 8)
    }
}

struct SwiftUIView_Previews: PreviewProvider {
    static var previews: some View {
        AddFeedScreen()
    }
}
