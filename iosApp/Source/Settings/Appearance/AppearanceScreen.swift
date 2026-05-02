import FeedFlowKit
import SwiftUI

struct AppearanceScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<MainSettingsViewModel>(
        Deps.shared.getMainSettingsViewModel()
    )

    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = MainSettingsState(themeMode: .system)

    var body: some View {
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
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.settingsAppearance))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            for await state in vmStoreOwner.instance.settingsState {
                self.settingsState = state
            }
        }
    }
}
