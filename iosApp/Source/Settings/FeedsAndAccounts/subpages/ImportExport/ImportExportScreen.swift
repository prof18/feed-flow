//
//  ImportExportScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/09/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

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

    var showCloseButton = false
    let fetchFeeds: () -> Void

    var body: some View {
        @Bindable var appState = appState

        NavigationStack {
            ImportExportContent(
                feedImportExportState: $feedImportExportState,
                onImportClick: {
                    sheetToShow = .filePicker
                },
                onExportClick: {
                    vmStoreOwner.instance.startExport()
                    if let url = getUrlForOpmlExport() {
                        vmStoreOwner.instance.exportFeed(opmlOutput: OpmlOutput(url: url))
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
                        if let url = getUrlForOpmlExport() {
                            do {
                                let data = try Data(contentsOf: url)
                                self.exportDocument = ExportDocument(data: data)
                                self.showFileExporter = true
                            } catch {
                                vmStoreOwner.instance.reportExportError()
                            }
                        }
                    }
                }
            }
            .sheet(item: $sheetToShow) { item in
                switch item {
                case .filePicker:
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
                }
            }
            .fileExporter(
                isPresented: $showFileExporter,
                document: exportDocument,
                contentType: .xml,
                defaultFilename: getDefaultFilename()
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

    private func getDefaultFilename() -> String {
        let formattedDate = vmStoreOwner.instance.getCurrentDateForExport()
        let deviceName = UIDevice.current.name.replacingOccurrences(of: " ", with: "-")
        return "feedflow-export_\(formattedDate)_\(deviceName).opml".lowercased()
    }
}
