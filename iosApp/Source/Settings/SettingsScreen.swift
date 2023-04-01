//
//  SettingsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct SettingsScreen: View {
    
    @EnvironmentObject var appState: AppState
    @State var sheetToShow: SheetToShow?
    @Environment(\.presentationMode) var presentationMode
    @StateObject var settingsViewModel: SettingsViewModel = KotlinDependencies.shared.getSettingsViewModel()
    
    var body: some View {
        ZStack {
            VStack {
                Form {
                    
                    HStack {
                        Button(
                            "Import Feed from OPML",
                            action: {
                                self.sheetToShow = .filePicker
                            }
                        )
                    }
                    
                    HStack {
                        Text("Add feed")
                    }
                    
                    HStack {
                        Text("Feeds")
                    }
                }
            }
            
            VStack(spacing: 0) {
                
                Spacer()
                
                Snackbar(snackbarData: $appState.snackbarDataForSheet)
            }
        }
        .sheet(item: $sheetToShow) { item in
            
            switch item {
                
            case .filePicker:
                FilePickerController { url in
                    // TODO
                    settingsViewModel.importFeed(opmlInput: OPMLInput(opmlData: url))
                    
                    self.appState.snackbarDataForSheet = SnackbarData(
                        title: "Importing feed",
                        subtitle: nil,
                        showBanner: true
                    )
                    
                    // TODO: close tab?
                }
            }
        }.task {
            do {
                let stream = asyncStream(for: settingsViewModel.isImportDoneStateNative)
                for try await isImportDone in stream {
                    if isImportDone as! Bool {
                        // TODO: maybe show in the global snackbar data
                        self.appState.snackbarDataForSheet = SnackbarData(
                            title: "Import done",
                            subtitle: nil,
                            showBanner: true
                        )
                    }
                }
            } catch {
                self.appState.snackbarDataForSheet = SnackbarData(
                    title: "Sorry, something went wrong :(",
                    subtitle: nil,
                    showBanner: true
                )
            }
        }
    }
}

struct SettingsScreen_Previews: PreviewProvider {
    static var previews: some View {
        SettingsScreen()
    }
}
