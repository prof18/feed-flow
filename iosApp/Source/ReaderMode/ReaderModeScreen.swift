import FeedFlowKit
import Foundation
import Reader
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
    @State private var readerExtractor: ReaderExtractorType?

    @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    let feedItemUrlInfo: FeedItemUrlInfo

    var body: some View {
        Group {
            if readerExtractor == nil {
                ProgressView()
            } else {
                ReaderView(
                    url: URL(string: feedItemUrlInfo.url)!,
                    options: ReaderViewOptions(
                        additionalCSS: """
                            #__reader_container {
                                font-size: \(fontSize)px
                            }
                        """,
                        onLinkClicked: { url in
                            if browserSelector.openInAppBrowser() {
                                browserToOpen = .inAppBrowser(url: url)
                            } else {
                                openURL(
                                    browserSelector.getUrlForDefaultBrowser(
                                        stringUrl: url.absoluteString))
                            }
                        },
                        readerExtractor: readerExtractor ?? .postlight
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
                        self.reset.toggle()
                    }
                }
            }
        }
        .task {
            for await state in vmStoreOwner.instance.readerExtractorState {
                switch state {
                case .postlight:
                    self.readerExtractor = .postlight
                case .defuddle:
                    self.readerExtractor = .defuddle
                case .none:
                    self.readerExtractor = .postlight
                }
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
                        vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                    } label: {
                        Image(systemName: "minus")
                    }

                    Slider(
                        value: $fontSize,
                        in: 12 ... 40,
                        onEditingChanged: { isEditing in
                            if !isEditing {
                                vmStoreOwner.instance.updateFontSize(
                                    newFontSize: Int32(Int(fontSize)))
                            }
                        }
                    )

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
