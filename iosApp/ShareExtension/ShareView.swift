//
//  ShareView.swift
//  ShareExtension
//
//  Created by Marco Gomiero on 06/01/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import SwiftUI
import UniformTypeIdentifiers

struct ShareView: View {
    let extensionContext: NSExtensionContext?

    enum UIState {
        case loading
        case success(message: String)
        case error(message: String)
        case idle
    }

    @StateObject private var vmStoreOwner = VMStoreOwner<AddFeedViewModel>(Deps.shared.getAddFeedViewModel())

    @State private var uiState: UIState = .loading

    var body: some View {
        VStack(spacing: Spacing.regular) {
            switch uiState {
            case .loading:
                ProgressView()
                    .scaleEffect(1.5)
                    .padding(.bottom, Spacing.small)
                Text(feedFlowStrings.addingFeed)
                    .font(.headline)

            case let .error(message):
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 50))
                    .foregroundStyle(.red)
                Text(message)
                    .font(.headline)

                doneButton

            case let .success(message):
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 50))
                    .foregroundStyle(.green)
                Text(message)
                    .font(.headline)

                doneButton

            case .idle:
                // Empty state, should not be visible
                EmptyView()
            }
        }
        .padding()
        .multilineTextAlignment(.center)
        .onAppear {
            extractSharedItem(extensionContext: extensionContext)
        }
        .task {
            for await state in vmStoreOwner.instance.feedAddedState {
                switch onEnum(of: state) {
                case let .feedAdded(addedState):
                    let message: String
                    if let feedName = addedState.feedName {
                        message = feedFlowStrings.feedAddedMessage(feedName)
                    } else {
                        message = feedFlowStrings.feedAddedMessageWithoutName
                    }

                    self.uiState = .success(message: message)

                case .feedNotAdded:
                    self.uiState = .loading

                case let .error(errorState):
                    let errorMessage: String
                    switch onEnum(of: errorState) {
                    case .invalidUrl:
                        errorMessage = feedFlowStrings.invalidRssUrl
                    case .invalidTitleLink:
                        errorMessage = feedFlowStrings.missingTitleAndLink
                    case .genericError:
                        errorMessage = feedFlowStrings.addFeedGenericError
                    }

                    self.uiState = .error(message: errorMessage)

                case .loading:
                    self.uiState = .loading
                }
            }
        }
    }

    private var doneButton: some View {
        Button(
            action: {
                self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
            },
            label: {
                Text(feedFlowStrings.actionDone)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .clipShape(.rect(cornerRadius: 10))
            }
        )
        .padding(.horizontal, Spacing.regular)
        .padding(.top, Spacing.small)
    }

    func extractSharedItem(extensionContext: NSExtensionContext?) {
        let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem
        let extensionAttachments = extensionItem?.attachments

        guard let attachments = extensionAttachments else {
            uiState = .error(message: feedFlowStrings.errorFeedAdd)
            return
        }
        let urlType = UTType.url.identifier
        guard let attachment = attachments.first(where: {
            $0.hasItemConformingToTypeIdentifier(urlType)
        }) else {
            uiState = .error(message: feedFlowStrings.errorFeedAdd)
            return
        }
        let errorMessage = feedFlowStrings.errorFeedAdd
        let viewModel = vmStoreOwner.instance

        attachment.loadItem(
            forTypeIdentifier: urlType,
            options: nil
        ) { item, error in
            DispatchQueue.main.async {
                guard error == nil, let url = item as? URL else {
                    self.uiState = .error(message: errorMessage)
                    return
                }

                viewModel.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: url.absoluteString)
                viewModel.addFeed()
            }
        }
    }
}
