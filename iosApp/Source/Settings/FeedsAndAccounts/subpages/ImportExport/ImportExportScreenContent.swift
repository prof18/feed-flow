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
    @Binding var articleExportFilter: ArticleExportFilter

    @State private var showExportArticlesDialog = false
    @State private var pendingArticleExportFilter: ArticleExportFilter = .all

    let onImportClick: () -> Void
    let onExportClick: () -> Void
    let onImportArticlesClick: () -> Void
    let onExportArticlesClick: (ArticleExportFilter) -> Void
    let onRetryClick: () -> Void
    let onDoneClick: () -> Void

    private let articleExportFilters: [ArticleExportFilter] = [
        .all,
        .read,
        .unread,
        .bookmarked
    ]

    var body: some View {
        importExportContent
    }
}

extension ImportExportContent {
    @ViewBuilder var importExportContent: some View {
        switch onEnum(of: feedImportExportState) {
        case .idle:
            idleView

        case .error:
            errorView

        case let .loadingImport(state):
            makeLoadingView(message: loadingMessage(for: state.contentType))

        case .loadingExport:
            makeLoadingView(message: feedFlowStrings.exportStartedMessage)

        case .exportSuccess:
            exportDoneView

        case .articleExportSuccess:
            articleExportDoneView

        case let .importSuccess(state):
            makeImportDoneView(state: state)

        case .articleImportSuccess:
            articleImportDoneView()
        }
    }
}

private extension ImportExportContent {
    @ViewBuilder private var idleView: some View {
        VStack {
            Form {
                Section(header: Text(feedFlowStrings.importExportOpmlSectionTitle)) {
                    Text(feedFlowStrings.importExportDescription)
                        .font(.body)
                        .multilineTextAlignment(.leading)

                    Button(
                        action: onImportClick,
                        label: {
                            Label(feedFlowStrings.importFeedButton, systemImage: "arrow.down.doc")
                        }
                    )

                    Button(action: onExportClick) {
                        Label(feedFlowStrings.exportFeedsButton, systemImage: "arrow.up.doc")
                    }
                }

                Section(header: Text(feedFlowStrings.importExportArticlesSectionTitle)) {
                    Text(feedFlowStrings.importExportArticlesDescription)
                        .font(.body)
                        .multilineTextAlignment(.leading)

                    Button(
                        action: onImportArticlesClick,
                        label: {
                            Label(feedFlowStrings.importArticlesButton, systemImage: "arrow.down.doc")
                        }
                    )

                    Button(
                        action: {
                            pendingArticleExportFilter = articleExportFilter
                            showExportArticlesDialog = true
                        },
                        label: {
                            Label(feedFlowStrings.exportArticlesButton, systemImage: "arrow.up.doc")
                        }
                    )
                }
            }
            Spacer()
        }
        .sheet(isPresented: $showExportArticlesDialog) {
            exportArticlesDialog
        }
    }

    private var exportArticlesDialog: some View {
        NavigationStack {
            Form {
                Section(header: Text(feedFlowStrings.articlesExportFilterTitle)) {
                    Picker(
                        selection: $pendingArticleExportFilter,
                        label: Text(feedFlowStrings.articlesExportFilterTitle)
                    ) {
                        ForEach(articleExportFilters.indices, id: \.self) { index in
                            let filter = articleExportFilters[index]
                            Text(articleExportFilterLabel(filter))
                                .tag(filter)
                        }
                    }
                }
            }
            .navigationTitle(feedFlowStrings.articlesExportFilterTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(feedFlowStrings.cancelButton) {
                        showExportArticlesDialog = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(feedFlowStrings.confirmButton) {
                        articleExportFilter = pendingArticleExportFilter
                        showExportArticlesDialog = false
                        onExportArticlesClick(pendingArticleExportFilter)
                    }
                }
            }
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
}

private extension ImportExportContent {
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

    @ViewBuilder private var articleExportDoneView: some View {
        VStack {
            Spacer()

            Text(feedFlowStrings.articlesExportDoneMessage)
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

    @ViewBuilder
    private func articleImportDoneView() -> some View {
        VStack {
            Spacer()

            Text(feedFlowStrings.articlesImportDoneMessage)
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

private extension ImportExportContent {
    private func articleExportFilterLabel(_ filter: ArticleExportFilter) -> String {
        switch filter {
        case .all:
            return feedFlowStrings.articlesExportFilterAll
        case .read:
            return feedFlowStrings.articlesExportFilterRead
        case .unread:
            return feedFlowStrings.articlesExportFilterUnread
        case .bookmarked:
            return feedFlowStrings.articlesExportFilterBookmarked
        }
    }

    private func loadingMessage(for contentType: ImportExportContentType) -> String {
        switch contentType {
        case .feedsOpml:
            return feedFlowStrings.feedAddInProgressMessage
        case .articlesCsv:
            return feedFlowStrings.articlesImportingMessage
        }
    }
}
