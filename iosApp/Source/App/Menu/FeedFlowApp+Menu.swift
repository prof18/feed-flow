import FeedFlowKit
import SwiftUI

extension FeedFlowApp {
    @CommandsBuilder var appMenu: some Commands {
        let feedFlowStrings = Deps.shared.getStrings()

        CommandGroup(replacing: .appSettings) {
            Button(feedFlowStrings.settingsButton) {
                NotificationCenter.default.post(name: .feedFlowOpenSettings, object: nil)
            }
            .keyboardShortcut(",", modifiers: [.command])
        }

        CommandGroup(replacing: .newItem) {
            Button(feedFlowStrings.refreshFeeds) {
                NotificationCenter.default.post(name: .feedFlowRefreshFeeds, object: nil)
            }
            .keyboardShortcut("r", modifiers: [.command])

            Button(feedFlowStrings.forceFeedRefresh) {
                NotificationCenter.default.post(name: .feedFlowForceRefreshFeeds, object: nil)
            }
            .keyboardShortcut("r", modifiers: [.command, .shift])

            Divider()

            Button(feedFlowStrings.markAllReadButton) {
                NotificationCenter.default.post(name: .feedFlowMarkAllRead, object: nil)
            }
            .keyboardShortcut("a", modifiers: [.command, .shift])

            Button(feedFlowStrings.clearOldArticlesButton) {
                NotificationCenter.default.post(name: .feedFlowClearOldArticles, object: nil)
            }
            .keyboardShortcut("d", modifiers: [.command, .shift])
        }

        CommandMenu(feedFlowStrings.feedsTitle) {
            Button(feedFlowStrings.addFeed) {
                NotificationCenter.default.post(name: .feedFlowAddFeed, object: nil)
            }
            .keyboardShortcut("n", modifiers: [.command])

            Button(feedFlowStrings.editFeed) {
                NotificationCenter.default.post(name: .feedFlowEditCurrentFeed, object: nil)
            }

            Divider()

            Button(feedFlowStrings.importExportLabel) {
                NotificationCenter.default.post(name: .feedFlowImportExport, object: nil)
            }
        }
    }
}
