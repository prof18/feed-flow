import FeedFlowKit
import SwiftUI

struct SettingsScreen: View {
    @Environment(AppState.self) private var appState
    @Environment(\.dismiss) private var dismiss
    @StateObject private var vmStoreOwner = VMStoreOwner<SettingsViewModel>(Deps.shared.getSettingsViewModel())
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = SettingsState(
        isMarkReadWhenScrollingEnabled: true,
        isShowReadItemsEnabled: false,
        isReaderModeEnabled: false,
        isSaveReaderModeContentEnabled: false,
        isPrefetchArticleContentEnabled: false,
        isExperimentalParsingEnabled: false,
        isRemoveTitleFromDescriptionEnabled: false,
        isHideDescriptionEnabled: false,
        isHideImagesEnabled: false,
        isHideDateEnabled: false,
        autoDeletePeriod: .disabled,
        isCrashReportingEnabled: true,
        syncPeriod: .oneHour,
        leftSwipeActionType: .none,
        rightSwipeActionType: .none,
        dateFormat: .normal,
        timeFormat: .hours24,
        feedOrder: .newestFirst,
        feedLayout: .list,
        themeMode: .system
    )
    let fetchFeeds: () -> Void

    var body: some View {
        NavigationStack {
            @Bindable var appState = appState

            Form {
                Section {
                    Picker(selection: Binding(
                        get: { settingsState.themeMode },
                        set: { newValue in
                            vmStoreOwner.instance.updateThemeMode(mode: newValue)
                            withAnimation(.easeInOut(duration: 0.3)) {
                                appState.updateTheme(newValue)
                            }
                        }
                    )) {
                        Text(feedFlowStrings.settingsThemeSystem)
                            .tag(ThemeMode.system)
                        Text(feedFlowStrings.settingsThemeLight)
                            .tag(ThemeMode.light)
                        Text(feedFlowStrings.settingsThemeDark)
                            .tag(ThemeMode.dark)
                    } label: {
                        Label(feedFlowStrings.settingsTheme, systemImage: "moon")
                    }

                    NavigationLink(destination: FeedsAndAccountsScreen(
                        fetchFeeds: fetchFeeds,
                        onClose: { dismiss() }
                    )) {
                        Label(feedFlowStrings.settingsFeedsAndAccounts, systemImage: "arrow.triangle.2.circlepath")
                    }

                    NavigationLink(destination: FeedListSettingsScreen()) {
                        Label(feedFlowStrings.settingsFeedListTitle, systemImage: "list.bullet.rectangle.portrait")
                    }

                    NavigationLink(destination: ReadingBehaviorScreen()) {
                        Label(feedFlowStrings.settingsReadingBehavior, systemImage: "book")
                    }

                    NavigationLink(destination: SyncAndStorageScreen()) {
                        Label(feedFlowStrings.settingsSyncAndStorage, systemImage: "externaldrive")
                    }

                    NavigationLink(destination: AboutAndSupportScreen()) {
                        Label(feedFlowStrings.settingsAboutAndSupport, systemImage: "info.circle")
                    }
                } footer: {
                    if let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
                        Text(feedFlowStrings.aboutAppVersion(appVersion))
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, Spacing.small)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .toolbar {
                Button {
                    dismiss()
                } label: {
                    Text(feedFlowStrings.actionDone).bold()
                }
            }
            .navigationTitle(Text(feedFlowStrings.settingsTitle))
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.secondaryBackgroundColor)
        }
        .task {
            for await state in vmStoreOwner.instance.settingsState {
                self.settingsState = state
            }
        }
    }
}
