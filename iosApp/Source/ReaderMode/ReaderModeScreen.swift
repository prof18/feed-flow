import FeedFlowKit
import Foundation
import Reader
import SwiftUI

struct ReaderModeScreen: View {
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(\.openURL)
    private var openURL
    @Environment(AppState.self)
    private var appState

    @State private var showFontSizeMenu: Bool = false
    @State private var fontSize = 16.0
    @State private var isSliderMoving = false
    @State private var reset = false
    @State private var isBookmarked = false

    @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    let feedItemUrlInfo: FeedItemUrlInfo

    var body: some View {
        ReaderView(
            url: URL(string: feedItemUrlInfo.url) ?? URL(string: "about:blank") ?? URL(fileURLWithPath: ""),
            options: ReaderViewOptions(
                additionalCSS: """
                    #__reader_container {
                        font-size: \(fontSize)px
                    }
                """
            ) { url in
                if browserSelector.openInAppBrowser() {
                    appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                } else {
                    openURL(
                        browserSelector.getUrlForDefaultBrowser(
                            stringUrl: url.absoluteString))
                }
            }
        ) {
            Button {
                isBookmarked.toggle()
                vmStoreOwner.instance.updateBookmarkStatus(
                    feedItemId: FeedItemId(id: feedItemUrlInfo.id),
                    bookmarked: isBookmarked
                )
            } label: {
                if isBookmarked {
                    Image(systemName: "bookmark.slash")
                } else {
                    Image(systemName: "bookmark")
                }
            }

            ShareLink(
                item: URL(string: feedItemUrlInfo.url) ?? URL(string: "about:blank") ?? URL(fileURLWithPath: "")
            ) {
                Label("Share", systemImage: "square.and.arrow.up")
            }

            Button {
                let archiveUrlString = getArchiveISUrl(articleUrl: feedItemUrlInfo.url)
                if browserSelector.openInAppBrowser() {
                    if let url = URL(string: archiveUrlString) {
                        appState.navigate(
                            route: CommonViewRoute.inAppBrowser(url: url)
                        )
                    }
                } else {
                    if let url = URL(string: archiveUrlString) {
                        openURL(
                            browserSelector.getUrlForDefaultBrowser(
                                stringUrl: url.absoluteString))
                    }
                }
            } label: {
                Image(systemName: "hammer.fill")
            }

            Button {
                if browserSelector.openInAppBrowser() {
                    if let url = URL(string: feedItemUrlInfo.url) {
                        appState.navigate(
                            route: CommonViewRoute.inAppBrowser(url: url)
                        )
                    }
                } else {
                    if let url = URL(string: feedItemUrlInfo.url) {
                        openURL(
                            browserSelector.getUrlForDefaultBrowser(
                                stringUrl: url.absoluteString))
                    }
                }
            } label: {
                Image(systemName: "globe")
            }

            fontSizeMenu
        }
        .onAppear {
            isBookmarked = feedItemUrlInfo.isBookmarked
        }
        .id(reset)
        .task {
            for await state in vmStoreOwner.instance.readerFontSizeState {
                self.fontSize = Double(truncating: state)
                self.reset.toggle()
            }
        }
    }

    @ViewBuilder private var fontSizeMenu: some View {
        Button {
            showFontSizeMenu.toggle()
        } label: {
            Image(systemName: "textformat.size")
        }
        .font(.title3)
        .popover(isPresented: $showFontSizeMenu) {
            VStack(alignment: .leading) {
                Text(feedFlowStrings.readerModeFontSize)

                HStack {
                    Button {
                        fontSize -= 1.0
                        vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                    } label: {
                        Image(systemName: "minus")
                    }

                    Slider(
                        value: $fontSize,
                        in: 12 ... 40
                    ) { isEditing in
                        if !isEditing {
                            vmStoreOwner.instance.updateFontSize(
                                newFontSize: Int32(Int(fontSize)))
                        }
                    }

                    Button {
                        fontSize += 1.0
                        vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .frame(width: 250, height: 100)
            .padding(.horizontal, Spacing.regular)
            .presentationCompactAdaptation((.popover))
        }
    }
}
