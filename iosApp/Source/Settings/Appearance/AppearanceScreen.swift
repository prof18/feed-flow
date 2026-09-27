import FeedFlowKit
import SwiftUI

struct AppearanceScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<MainSettingsViewModel>(
        Deps.shared.getMainSettingsViewModel()
    )

    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = MainSettingsState(
        themeMode: .system,
        isHideUnreadCountEnabled: false,
        bodyFont: .system,
        headlineFont: .system
    )

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
                .accessibilityIdentifier(AppearanceAccessibilityIdentifiers.themePicker)

                Picker(selection: Binding(
                    get: { settingsState.headlineFont },
                    set: { vmStoreOwner.instance.updateHeadlineFont(font: $0) }
                )) {
                    ForEach(Self.readerFontFamilies, id: \.name) { font in
                        Text(Self.displayName(for: font, strings: feedFlowStrings))
                            .tag(font)
                    }
                } label: {
                    Label(feedFlowStrings.readerModeHeadlineFont, systemImage: "textformat.size")
                }

                Picker(selection: Binding(
                    get: { settingsState.bodyFont },
                    set: { vmStoreOwner.instance.updateBodyFont(font: $0) }
                )) {
                    ForEach(Self.readerFontFamilies, id: \.name) { font in
                        Text(Self.displayName(for: font, strings: feedFlowStrings))
                            .tag(font)
                    }
                } label: {
                    Label(feedFlowStrings.readerModeBodyFont, systemImage: "textformat")
                }

                Toggle(isOn: Binding(
                    get: { settingsState.isHideUnreadCountEnabled },
                    set: { vmStoreOwner.instance.updateHideUnreadCount(value: $0) }
                )) {
                    Text(feedFlowStrings.settingsHideUnreadCount)
                }
                .accessibilityIdentifier(AppearanceAccessibilityIdentifiers.hideUnreadCountToggle)
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

    private static let readerFontFamilies: [ReaderFontFamily] = [
        .system,
        .outfit,
        .inter,
        .atkinsonHyperlegible,
        .literata,
        .sourceSerif4,
        .libreBaskerville,
        .lora
    ]

    private static func displayName(for font: ReaderFontFamily, strings: FeedFlowStrings) -> String {
        switch font {
        case .system: strings.readerFontSystem
        case .outfit: strings.readerFontOutfit
        case .inter: strings.readerFontInter
        case .atkinsonHyperlegible: strings.readerFontAtkinsonHyperlegible
        case .literata: strings.readerFontLiterata
        case .sourceSerif4: strings.readerFontSourceSerif4
        case .libreBaskerville: strings.readerFontLibreBaskerville
        case .lora: strings.readerFontLora
        default: font.name
        }
    }
}
