import FeedFlowKit
import SwiftUI

struct AppearanceScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<MainSettingsViewModel>(
        Deps.shared.getMainSettingsViewModel()
    )

    private let feedFlowStrings = Deps.shared.getStrings()
    private let homeSettingsRepository = Deps.shared.getIosHomeSettingsRepository()

    @State private var settingsState = MainSettingsState(themeMode: .system)
    @State private var isMultiPaneEnabled = true

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

                SettingToggleItem(
                    isOn: Binding(
                        get: { isMultiPaneEnabled },
                        set: { newValue in
                            isMultiPaneEnabled = newValue
                            homeSettingsRepository.setMultiPaneLayoutEnabled(value: newValue)
                        }
                    ),
                    title: feedFlowStrings.settingsThreePaneLayout,
                    systemImage: "rectangle.split.2x1"
                )
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.settingsAppearance))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            isMultiPaneEnabled = homeSettingsRepository.isMultiPaneLayoutEnabled()
        }
        .task {
            for await state in vmStoreOwner.instance.settingsState {
                self.settingsState = state
            }
        }
        .task {
            for await value in homeSettingsRepository.isMultiPaneLayoutEnabledFlow {
                isMultiPaneEnabled = (value as? Bool) ?? true
            }
        }
    }
}
