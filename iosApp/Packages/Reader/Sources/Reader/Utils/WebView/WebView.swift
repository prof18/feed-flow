import Combine
import SwiftUI
import WebKit

enum WebViewEvent: Equatable {
    struct ScrollInfo: Equatable {
        var contentOffset: CGPoint
        var contentSize: CGSize
    }

    case scrolledDown
    case scrolledUp
    case scrollPositionChanged(ScrollInfo)
}

#if os(macOS)
    struct WebView: NSViewRepresentable {
        typealias NSViewType = _WebViewContainer

        var content: WebContent
        var onEvent: ((WebViewEvent) -> Void)?

        func makeNSView(context _: Context) -> _WebViewContainer {
            return _WebViewContainer()
        }

        func updateNSView(_ nsView: _WebViewContainer, context _: Context) {
            nsView.contentView = (content.view as! WKWebView)
            nsView.onEvent = onEvent
        }
    }

    class _WebViewContainer: NSView {
        var onEvent: ((WebViewEvent) -> Void)?
        // TODO: Implement scroll  events
        private var webviewSubs = Set<NSKeyValueObservation>()

        var contentView: WKWebView? {
            didSet(old) {
                guard contentView != old else { return }
                webviewSubs.removeAll()
                old?.removeFromSuperview()

                if let view = contentView {
                    addSubview(view)
                }
            }
        }

        override func layout() {
            super.layout()
            contentView?.frame = bounds
        }
    }

#else
    struct WebView: UIViewRepresentable {
        typealias UIViewType = _WebViewContainer

        var content: WebContent
        var onEvent: ((WebViewEvent) -> Void)?

        func makeUIView(context _: Context) -> _WebViewContainer {
            return _WebViewContainer()
        }

        func updateUIView(_ uiView: _WebViewContainer, context _: Context) {
            uiView.contentView = (content.view as! WKWebView)
            uiView.onEvent = onEvent
        }
    }

    class _WebViewContainer: UIView {
        var onEvent: ((WebViewEvent) -> Void)?

        var scrollPosRounded: CGFloat = 0 {
            didSet(old) {
                guard scrollPosRounded != old else { return }
                if scrollPosRounded < 50 {
                    scrollDirection = -1 // up
                } else {
                    scrollDirection = scrollPosRounded > old ? 1 : -1
                }
            }
        }

        var scrollDirection = 0 {
            didSet(old) {
                guard scrollDirection != old else { return }
                if scrollDirection == 1 {
                    onEvent?(.scrolledDown)
                } else if scrollDirection == -1 {
                    onEvent?(.scrolledUp)
                }
            }
        }

        private var webviewSubs = Set<NSKeyValueObservation>()

        var contentView: WKWebView? {
            didSet(old) {
                guard contentView != old else { return }
                webviewSubs.removeAll()
                old?.removeFromSuperview()

                if let view = contentView {
                    addSubview(view)
                    webviewSubs.insert(view.scrollView.observe(\.contentOffset, options: [.new]) { [weak self] scrollView, _ in
                        self?.onEvent?(.scrollPositionChanged(scrollView.info))
                        let offset = scrollView.info.contentOffset
                        self?.scrollPosRounded = (offset.y / 40).rounded() * 40
                    })
                    webviewSubs.insert(view.scrollView.observe(\.contentSize, options: [.new]) { [weak self] scrollView, _ in
                        self?.onEvent?(.scrollPositionChanged(scrollView.info))
                    })
                }
            }
        }

        override func layoutSubviews() {
            super.layoutSubviews()
            contentView?.frame = bounds
        }
    }

    private extension UIScrollView {
        var info: WebViewEvent.ScrollInfo {
            return .init(contentOffset: contentOffset, contentSize: contentSize)
        }
    }
#endif
