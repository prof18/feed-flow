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

    @StateObject
    private var vmStoreOwner = VMStoreOwner<ImportExportViewModel>(KotlinDependencies.shared.getImportExportViewModel())

    @State var feedImportExportState: FeedImportExportState = FeedImportExportState.Idle()
    @State var sheetToShow: ImportExportSheetToShow?

    var showCloseButton: Bool = false

    var body: some View {
        NavigationStack {
            ImportExportContent(
                feedImportExportState: $feedImportExportState,
                sheetToShow: $sheetToShow,
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
                    vmStoreOwner.instance.clearState()
                }
            )
            .navigationTitle(feedFlowStrings.importExportOpmlTitle)
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
                for await state in vmStoreOwner.instance.importExportState {
                    self.feedImportExportState = state
                    if state is FeedImportExportState.ExportSuccess {
                        self.sheetToShow = .shareSheet
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
                            self.appState.snackbarQueue.append(
                                SnackbarData(
                                    title: feedFlowStrings.loadFileErrorMessage,
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

        let formattedDate = vmStoreOwner.instance.getCurrentDateForExport()
        let deviceName = UIDevice.current.name.replacingOccurrences(of: " ", with: "-")
        let fileName = "feedflow-export_\(formattedDate)_\(deviceName).opml".lowercased()

        return cacheDirectory?.appendingPathComponent(fileName)
    }
}
