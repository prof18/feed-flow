//
//  ImportExportScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct ImportExportContent: View {
    @Environment(\.presentationMode)
    var presentationMode

    @Binding var feedImportExportState: FeedImportExportState
    @Binding var savedUrls: [String]

    let onImportClick: () -> Void
    let onExportClick: () -> Void
    let onRetryClick: () -> Void
    let onDoneClick: () -> Void
    let onImportFromUrlClick: (String) -> Void
    let onReimportFromUrlClick: (String) -> Void
    let onDeleteUrlClick: (String) -> Void

    @State private var urlInput: String = ""

    var body: some View {
        switch onEnum(of: feedImportExportState) {
        case .idle:
            idleView

        case .error:
            errorView

        case .loadingImport:
            makeLoadingView(message: feedFlowStrings.feedAddInProgressMessage)

        case .loadingExport:
            makeLoadingView(message: feedFlowStrings.exportStartedMessage)

        case .exportSuccess:
            exportDoneView

        case let .importSuccess(state):
            makeImportDoneView(state: state)
        }
    }

    @ViewBuilder var importExportContent: some View {
        switch onEnum(of: feedImportExportState) {
        case .idle:
            idleView

        case .error:
            errorView

        case .loadingImport:
            makeLoadingView(message: feedFlowStrings.feedAddInProgressMessage)

        case .loadingExport:
            makeLoadingView(message: feedFlowStrings.exportStartedMessage)

        case .exportSuccess:
            exportDoneView

        case let .importSuccess(state):
            makeImportDoneView(state: state)
        }
    }

    @ViewBuilder private var idleView: some View {
        VStack {
            Form {
                Section {
                    Text(feedFlowStrings.importExportDescription)
                        .font(.body)
                        .multilineTextAlignment(.leading)
                }

                Button(
                    action: onImportClick,
                    label: {
                        Label(feedFlowStrings.importFeedButton, systemImage: "arrow.down.doc")
                    }
                )

                Button(action: onExportClick) {
                    Label(feedFlowStrings.exportFeedsButton, systemImage: "arrow.up.doc")
                }

                Section {
                    Text(feedFlowStrings.importFromUrlTitle)
                        .font(.headline)
                } header: {
                    EmptyView()
                }

                HStack {
                    TextField(feedFlowStrings.importFromUrlHint, text: $urlInput)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .autocapitalization(.none)
                    Button(action: {
                        if !urlInput.isEmpty {
                            onImportFromUrlClick(urlInput)
                            urlInput = ""
                        }
                    }) {
                        Text(feedFlowStrings.importFromUrlButton)
                    }
                    .buttonStyle(.bordered)
                }

                if !savedUrls.isEmpty {
                    Section {
                        Text(feedFlowStrings.savedOpmlUrlsTitle)
                            .font(.headline)
                    } header: {
                        EmptyView()
                    }

                    ForEach(savedUrls, id: \.self) { url in
                        HStack {
                            Text(url)
                                .font(.body)
                                .lineLimit(1)

                            Spacer()

                            Button(action: {
                                onReimportFromUrlClick(url)
                            }) {
                                Image(systemName: "arrow.clockwise")
                            }

                            Button(action: {
                                onDeleteUrlClick(url)
                            }) {
                                Image(systemName: "trash")
                                    .foregroundColor(.red)
                            }
                        }
                    }
                }
            }
            Spacer()
        }
    }

    @ViewBuilder private var errorView: some View {
        VStack {
            Spacer()

            Text(feedFlowStrings.genericErrorMessage)
                .font(.body)

            Button(action: onRetryClick) {
                Text(feedFlowStrings.retryButton)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.medium)

            Spacer()
        }.frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private func makeLoadingView(message: String) -> some View {
        VStack {
            Spacer()
            ProgressView()
            Text(message)
                .padding(Spacing.regular)
                .font(.body)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder private var exportDoneView: some View {
        VStack {
            Spacer()

            Text(feedFlowStrings.feedsExportDoneMessage)
                .font(.body)
                .multilineTextAlignment(.center)

            Button(
                action: {
                    onDoneClick()
                    self.presentationMode.wrappedValue.dismiss()
                },
                label: {
                    Text(feedFlowStrings.doneButton)
                        .frame(maxWidth: .infinity)
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.medium)

            Spacer()
        }
    }

    @ViewBuilder
    private func makeImportDoneView(state: FeedImportExportState.ImportSuccess) -> some View {
        VStack {
            if !state.notValidFeedSources.isEmpty {
                makeInvalidFeedSourceView(state: state)
            } else if !state.feedSourceWithError.isEmpty {
                makeErrorFeedSourceView(state: state)
            } else {
                allFeedSourceValidView
            }
        }
    }

    @ViewBuilder private var allFeedSourceValidView: some View {
        Spacer()

        Text(feedFlowStrings.feedsImportDoneMessage)
            .font(.body)
            .multilineTextAlignment(.center)

        Button(
            action: {
                onDoneClick()
                self.presentationMode.wrappedValue.dismiss()
            },
            label: {
                Text(feedFlowStrings.doneButton)
                    .frame(maxWidth: .infinity)
            }
        )
        .buttonStyle(.bordered)
        .padding(.top, Spacing.regular)
        .padding(.horizontal, Spacing.medium)

        Spacer()
    }

    @ViewBuilder
    private func makeErrorFeedSourceView(state: FeedImportExportState.ImportSuccess) -> some View {
        Text(feedFlowStrings.linkWithErrorReportTitle)
            .font(.body)
            .padding(Spacing.regular)

        List {
            ForEach(state.feedSourceWithError, id: \.self.url) { feedSource in
                VStack(alignment: .leading) {
                    Text(feedSource.title)
                        .font(.system(size: 16))
                        .padding(.top, Spacing.xsmall)

                    Text(feedSource.url)
                        .font(.system(size: 14))
                        .padding(.top, Spacing.xxsmall)
                        .padding(.bottom, Spacing.xsmall)
                }
            }
        }
        .listStyle(PlainListStyle())

        Spacer()

        Button {
            onDoneClick()
            self.presentationMode.wrappedValue.dismiss()
        } label: {
            Text(feedFlowStrings.doneButton)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.bordered)
        .padding(Spacing.regular)
    }

    @ViewBuilder
    private func makeInvalidFeedSourceView(state: FeedImportExportState.ImportSuccess) -> some View {
        Text(feedFlowStrings.wrongLinkReportTitle)
            .font(.body)
            .padding(Spacing.regular)

        List {
            ForEach(state.notValidFeedSources, id: \.self.url) { feedSource in
                VStack(alignment: .leading) {
                    Text(feedSource.title)
                        .font(.system(size: 16))
                        .padding(.top, Spacing.xsmall)

                    Text(feedSource.url)
                        .font(.system(size: 14))
                        .padding(.top, Spacing.xxsmall)
                        .padding(.bottom, Spacing.xsmall)
                }
            }
        }
        .listStyle(PlainListStyle())

        Spacer()

        Button {
            onDoneClick()
            self.presentationMode.wrappedValue.dismiss()
        } label: {
            Text(feedFlowStrings.doneButton)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.bordered)
        .padding(Spacing.regular)
    }
}

#Preview("With error") {
    ImportExportContent(
        feedImportExportState: .constant(feedImportSuccessWithErrorState),
        savedUrls: .constant(["https://example.com/feeds.opml"]),
        onImportClick: {},
        onExportClick: {},
        onRetryClick: {},
        onDoneClick: {},
        onImportFromUrlClick: { _ in },
        onReimportFromUrlClick: { _ in },
        onDeleteUrlClick: { _ in }
    )
}

#Preview {
    ImportExportContent(
        feedImportExportState: .constant(feedImportSuccessState),
        savedUrls: .constant([]),
        onImportClick: {},
        onExportClick: {},
        onRetryClick: {},
        onDoneClick: {},
        onImportFromUrlClick: { _ in },
        onReimportFromUrlClick: { _ in },
        onDeleteUrlClick: { _ in }
    )
}
