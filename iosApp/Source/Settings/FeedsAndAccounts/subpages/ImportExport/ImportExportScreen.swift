//
//  ImportExportScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/09/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI
import UniformTypeIdentifiers

struct ImportExportScreen: View {
    @Environment(\.presentationMode)
    private var presentationMode

    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<ImportExportViewModel>(Deps.shared.getImportExportViewModel())

    @State var feedImportExportState: FeedImportExportState = .Idle()
    @State private var sheetToShow: ImportExportSheetToShow?
    @State private var showFileExporter = false
    @State private var exportDocument: ExportDocument?
    @State private var exportContentType: UTType = .xml
    @State private var exportDefaultFileName = ""
    @State private var articleExportFilter: ArticleExportFilter = .all

    var showCloseButton = false
    let fetchFeeds: () -> Void

    var body: some View {
        @Bindable var appState = appState

        NavigationStack {
            ImportExportContent(
                feedImportExportState: $feedImportExportState,
                articleExportFilter: $articleExportFilter,
                onImportClick: {
                    sheetToShow = .opmlFilePicker
                },
                onExportClick: {
                    vmStoreOwner.instance.startExport(contentType: .feedsOpml)
                    if let url = getUrlForOpmlExport() {
                        vmStoreOwner.instance.exportFeed(opmlOutput: OpmlOutput(url: url))
                    } else {
                        vmStoreOwner.instance.reportExportError()
                    }
                },
                onImportArticlesClick: {
                    sheetToShow = .csvFilePicker
                },
                onExportArticlesClick: { filter in
                    vmStoreOwner.instance.startExport(contentType: .articlesCsv)
                    if let url = getUrlForArticlesExport(filter: filter) {
                        vmStoreOwner.instance.exportArticles(
                            csvOutput: CsvOutput(url: url),
                            filter: filter
                        )
                    } else {
                        vmStoreOwner.instance.reportExportError()
                    }
                },
                onRetryClick: {
                    vmStoreOwner.instance.clearState()
                },
                onDoneClick: {
                    fetchFeeds()
                    vmStoreOwner.instance.clearState()
                }
            )
            .snackbar(messageQueue: $appState.snackbarQueue)
            .navigationTitle(feedFlowStrings.importExportOpmlTitle)
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.secondaryBackgroundColor)
            .toolbar {
                if showCloseButton {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button {
                            presentationMode.wrappedValue.dismiss()
                        } label: {
                            if isiOS26OrLater() {
                                Image(systemName: "xmark")
                            } else {
                                Image(systemName: "xmark.circle")
                            }
                        }
                    }
                }
            }
            .task {
                for await state in vmStoreOwner.instance.importExportState {
                    self.feedImportExportState = state
                    if state is FeedImportExportState.ExportSuccess {
                        prepareExport(
                            url: getUrlForOpmlExport(),
                            contentType: .xml,
                            defaultFileName: getDefaultOpmlFilename()
                        )
                    } else if state is FeedImportExportState.ArticleExportSuccess {
                        prepareExport(
                            url: getUrlForArticlesExport(filter: articleExportFilter),
                            contentType: .commaSeparatedText,
                            defaultFileName: getDefaultArticlesFilename(filter: articleExportFilter)
                        )
                    }
                }
            }
            .sheet(item: $sheetToShow) { item in
                switch item {
                case .opmlFilePicker:
                    FilePickerController { url in
                        do {
                            let data = try Data(contentsOf: url)
                            vmStoreOwner.instance.importFeed(opmlInput: OpmlInput(opmlData: data))
                        } catch {
                            vmStoreOwner.instance.reportExportError()
                            appState.snackbarQueue.append(
                                SnackbarData(
                                    title: feedFlowStrings.loadFileErrorMessage,
                                    subtitle: nil,
                                    showBanner: true
                                )
                            )
                        }
                    }
                case .csvFilePicker:
                    FilePickerController { url in
                        do {
                            let data = try Data(contentsOf: url)
                            vmStoreOwner.instance.importArticles(csvInput: CsvInput(csvData: data))
                        } catch {
                            vmStoreOwner.instance.reportExportError()
                            appState.snackbarQueue.append(
                                SnackbarData(
                                    title: feedFlowStrings.loadFileErrorMessage,
                                    subtitle: nil,
                                    showBanner: true
                                )
                            )
                        }
                    }
                }
            }
            .fileExporter(
                isPresented: $showFileExporter,
                document: exportDocument,
                contentType: exportContentType,
                defaultFilename: exportDefaultFileName
            ) { result in
                switch result {
                case .success(let url):
                    print("File exported to: \(url)")
                case .failure(let error):
                    print("Export failed: \(error)")
                    vmStoreOwner.instance.reportExportError()
                }
            }
        }
    }

    private func getUrlForOpmlExport() -> URL? {
        let cacheDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first

        let formattedDate = vmStoreOwner.instance.getCurrentDateForExport()
        let deviceName = UIDevice.current.name.replacingOccurrences(of: " ", with: "-")
        let fileName = "feedflow-export_\(formattedDate)_\(deviceName).opml".lowercased()

        return cacheDirectory?.appendingPathComponent(fileName)
    }

    private func getDefaultOpmlFilename() -> String {
        let formattedDate = vmStoreOwner.instance.getCurrentDateForExport()
        let deviceName = UIDevice.current.name.replacingOccurrences(of: " ", with: "-")
        return "feedflow-export_\(formattedDate)_\(deviceName).opml".lowercased()
    }

    private func getUrlForArticlesExport(filter: ArticleExportFilter) -> URL? {
        let cacheDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first

        let formattedDate = vmStoreOwner.instance.getCurrentDateForExport()
        let deviceName = UIDevice.current.name.replacingOccurrences(of: " ", with: "-")
        let fileName = "feedflow-articles-export_\(formattedDate)_\(deviceName)_\(articleExportFilterSuffix(filter)).csv"

        return cacheDirectory?.appendingPathComponent(fileName)
    }

    private func getDefaultArticlesFilename(filter: ArticleExportFilter) -> String {
        let formattedDate = vmStoreOwner.instance.getCurrentDateForExport()
        let deviceName = UIDevice.current.name.replacingOccurrences(of: " ", with: "-")
        return "feedflow-articles-export_\(formattedDate)_\(deviceName)_\(articleExportFilterSuffix(filter)).csv"
    }

    private func prepareExport(
        url: URL?,
        contentType: UTType,
        defaultFileName: String
    ) {
        guard let url else {
            vmStoreOwner.instance.reportExportError()
            return
        }
        do {
            let data = try Data(contentsOf: url)
            exportDocument = ExportDocument(data: data)
            exportContentType = contentType
            exportDefaultFileName = defaultFileName
            showFileExporter = true
        } catch {
            vmStoreOwner.instance.reportExportError()
        }
    }

    private func articleExportFilterSuffix(_ filter: ArticleExportFilter) -> String {
        switch filter {
        case .all:
            return "all"
        case .read:
            return "read"
        case .unread:
            return "unread"
        case .bookmarked:
            return "bookmarked"
        }
    }
}
