//
//  ImportExportScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/09/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct ImportExportScreen: View {

    @Environment(\.presentationMode) private var presentationMode

    @EnvironmentObject private var appState: AppState

    @StateObject private var viewModel = KotlinDependencies.shared.getImportExportViewModel()

    @State var feedImportExportState: FeedImportExportState = FeedImportExportState.Idle()
    @State var sheetToShow: ImportExportSheetToShow?

    var showCloseButton: Bool = false

    var body: some View {
        NavigationStack {
            ImportExportContent(
                feedImportExportState: $feedImportExportState,
                sheetToShow: $sheetToShow,
                onExportClick: {
                    viewModel.startExport()
                    if let url = getUrlForOpmlExport() {
                        viewModel.exportFeed(opmlOutput: OpmlOutput(url: url))
                    } else {
                        viewModel.reportExportError()
                    }
                },
                onRetryClick: {
                    viewModel.clearState()
                },
                onDoneClick: {
                    viewModel.clearState()
                }
            )
            .navigationTitle(localizer.import_export_opml_title.localized)
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.secondaryBackgroundColor)
            .toolbar {
                if showCloseButton {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button {
                            presentationMode.wrappedValue.dismiss()
                        } label: {
                            Image(systemName: "xmark.circle")
                        }
                    }
                }
            }
            .task {
                do {
                    let stream = asyncSequence(for: viewModel.importExportStateFlow)
                    for try await state in stream {
                        self.feedImportExportState = state
                        if state is FeedImportExportState.ExportSuccess {
                            self.sheetToShow = .shareSheet
                        }
                    }
                } catch {
                    self.appState.emitGenericError()
                }
            }
            .sheet(item: $sheetToShow) { item in
                switch item {
                case .filePicker:
                    FilePickerController { url in
                        do {
                            let data = try Data(contentsOf: url)
                            viewModel.importFeed(opmlInput: OpmlInput(opmlData: data))
                        } catch {
                            viewModel.reportExportError()
                            self.appState.snackbarQueue.append(
                                SnackbarData(
                                    title: localizer.load_file_error_message.localized,
                                    subtitle: nil,
                                    showBanner: true
                                )
                            )
                        }
                    }

                case .shareSheet:
                    ShareSheet(
                        activityItems: [getUrlForOpmlExport()! as URL],
                        applicationActivities: nil
                    ) { _, _, _, _ in }
                }
            }
        }

    }

    private func getUrlForOpmlExport() -> URL? {
        let cacheDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
        return cacheDirectory?.appendingPathComponent("feed-export.opml")
    }
}
