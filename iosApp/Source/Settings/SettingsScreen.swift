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
        NavigationStack {
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
                    
                    NavigationLink(value: SheetPage.addFeed) {
                        HStack {
                            Text("Add feed")
                        }
                    }
                    
                    NavigationLink(value: SheetPage.feedList) {
                        HStack {
                            Text("Feeds")
                        }
                    }
                }
                ._safeAreaInsets(EdgeInsets(top: -30, leading: 0, bottom: 0, trailing: 0))
            }
            .navigationDestination(for: SheetPage.self) { page in
                switch page {
                case .addFeed:
                    AddFeedScreen()
                    
                case .feedList:
                    FeedsScreen()
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("Settings")
                        .font(.title2)
                        .padding(.vertical, Spacing.medium)
                    
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        self.presentationMode.wrappedValue.dismiss()
                    } label: {
                        Text("Close")
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
                        settingsViewModel.importFeed(opmlInput: OPMLInput(opmlData: data))
                    } catch {
                        self.appState.snackbarData = SnackbarData(
                            title: "Unable to load file",
                            subtitle: nil,
                            showBanner: true
                        )
                    }
                    
                    
                    
                    self.appState.snackbarData = SnackbarData(
                        title: "Importing feed",
                        subtitle: nil,
                        showBanner: true
                    )
                }
            }
        }.task {
            do {
                let stream = asyncStream(for: settingsViewModel.isImportDoneStateNative)
                for try await isImportDone in stream {
                    if isImportDone as! Bool {
                        self.appState.snackbarData = SnackbarData(
                            title: "Import done",
                            subtitle: nil,
                            showBanner: true
                        )
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            } catch {
                self.appState.snackbarData = SnackbarData(
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
