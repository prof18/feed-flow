import FeedFlowKit
import Foundation
import Reeeed
import SwiftUI

struct ReaderModeScreen: View {
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(\.openURL) private var openURL

    @State private var showFontSizeMenu: Bool = false
    @State private var fontSize = 16.0
    @State private var isSliderMoving = false
    @State private var reset = false
    @State private var isBookmarked = false
    @State private var browserToOpen: BrowserToPresent?

    @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    let feedItemUrlInfo: FeedItemUrlInfo

    var body: some View {
        ReeeederView(
            url: URL(string: feedItemUrlInfo.url)!,
            options: ReeeederViewOptions(
                theme: .init(
                    additionalCSS: """
                        #__reader_container {
                            font-size: \(fontSize)px
                        }
                    """
                ),
                onLinkClicked: { url in
                    if browserSelector.openInAppBrowser() {
                        browserToOpen = .inAppBrowser(url: url)
                    } else {
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url.absoluteString))
                    }
                }
            ),
            toolbarContent: {
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
                    item: URL(string: feedItemUrlInfo.url)!,
                    label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                )

                Button {
                    if browserSelector.openInAppBrowser() {
                        browserToOpen = .inAppBrowser(url: URL(string: feedItemUrlInfo.url)!)
                    } else {
                        openURL(
                            browserSelector.getUrlForDefaultBrowser(
                                stringUrl: URL(string: feedItemUrlInfo.url)!.absoluteString))
                    }
                } label: {
                    Image(systemName: "globe")
                }

                fontSizeMenu
            }
        )
        .onAppear {
            isBookmarked = feedItemUrlInfo.isBookmarked
        }
        .id(reset)
        .fullScreenCover(item: $browserToOpen) { browserToOpen in
            switch browserToOpen {
            case let .inAppBrowser(url):
                SFSafariView(url: url)
                    .ignoresSafeArea()
            }
        }
        .task {
            for await state in vmStoreOwner.instance.readerFontSizeState {
                self.fontSize = Double(truncating: state)
            }
        }
    }

    @ViewBuilder
    private var fontSizeMenu: some View {
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
                        self.reset.toggle()
                        vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                    } label: {
                        Image(systemName: "minus")
                    }

                    Slider(
                        value: $fontSize,
                        in: 12 ... 40,
                        onEditingChanged: { isEditing in
                            if !isEditing {
                                self.reset.toggle()
                                vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                            }
                        }
                    )

                    Button {
                        fontSize += 1.0
                        self.reset.toggle()
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
