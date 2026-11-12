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
        case info(message: String)
        case error(message: String)
        case idle
    }

    @StateObject private var vmStoreOwner = VMStoreOwner<AddFeedViewModel>(Deps.shared.getAddFeedViewModel())

    @State private var uiState: UIState = .loading
    @State private var extractionProgress: Progress?
    @State private var extractionId: UUID?

    var body: some View {
        VStack(spacing: Spacing.regular) {
            switch uiState {
            case .loading:
                ProgressView()
                    .scaleEffect(1.5)
                    .padding(.bottom, Spacing.small)
                Text(feedFlowStrings.addingFeed)
                    .font(.headline)

                cancelButton

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

            case let .info(message):
                Image(systemName: "info.circle.fill")
                    .font(.system(size: 50))
                    .foregroundStyle(.secondary)
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
        .onDisappear {
            extractionId = nil
            extractionProgress?.cancel()
            extractionProgress = nil
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

                case let .feedAlreadyExists(existingState):
                    self.uiState = .info(message: feedFlowStrings.feedAlreadyExistsMessage(existingState.feedName))

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
                    case .fetchFailed:
                        errorMessage = feedFlowStrings.feedFetchRefusedError
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
                    .frame(maxWidth: .infinity)
            }
        )
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .padding(.horizontal, Spacing.regular)
        .padding(.top, Spacing.small)
    }

    private var cancelButton: some View {
        Button(feedFlowStrings.cancelButton) {
            extractionId = nil
            extractionProgress?.cancel()
            extensionContext?.cancelRequest(
                withError: NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError)
            )
        }
        .buttonStyle(.bordered)
        .controlSize(.large)
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
        let extractionId = UUID()
        self.extractionId = extractionId

        extractionProgress = attachment.loadObject(ofClass: NSURL.self) { item, error in
            DispatchQueue.main.async {
                guard self.extractionId == extractionId else { return }
                self.extractionProgress = nil
                guard error == nil, let sharedURL = item as? NSURL else {
                    self.uiState = .error(message: errorMessage)
                    return
                }
                let url = sharedURL as URL

                viewModel.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: url.absoluteString)
                viewModel.addFeed()
            }
        }
    }
}
