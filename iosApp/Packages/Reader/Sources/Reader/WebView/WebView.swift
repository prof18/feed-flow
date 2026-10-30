import Combine
import SwiftUI
import WebKit

struct WebView: UIViewRepresentable {
    var content: WebContent
    var onEvent: ((WebViewEvent) -> Void)?
    var topInset: CGFloat = 0

    func makeUIView(context _: Context) -> WebViewContainer {
        return WebViewContainer()
    }

    func updateUIView(_ uiView: WebViewContainer, context _: Context) {
        guard let webview = content.view as? WKWebView else { return }
        uiView.contentView = webview
        uiView.onEvent = onEvent
        uiView.topInset = topInset
    }
}

class WebViewContainer: UIView {
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

    /// Space for the floating bars: the web view runs beneath them and its scroll view starts the
    /// content below, since a hosted WKWebView does not pick up the SwiftUI safe area by itself.
    var topInset: CGFloat = 0 {
        didSet(old) {
            guard topInset != old else { return }
            applyTopInset(previous: old)
        }
    }

    var contentView: WKWebView? {
        didSet(old) {
            guard contentView != old else { return }
            webviewSubs.removeAll()
            old?.removeFromSuperview()

            if let view = contentView {
                addSubview(view)
                applyTopInset(previous: view.scrollView.contentInset.top)
                webviewSubs.insert(
                    view.scrollView.observe(\.contentOffset, options: [.new]) { [weak self] scrollView, _ in
                        self?.onEvent?(.scrollPositionChanged(scrollView.info))
                        let offset = scrollView.info.contentOffset
                        self?.scrollPosRounded = (offset.y / 40).rounded() * 40
                    }
                )
                webviewSubs.insert(
                    view.scrollView.observe(\.contentSize, options: [.new]) { [weak self] scrollView, _ in
                        self?.onEvent?(.scrollPositionChanged(scrollView.info))
                    }
                )
            }
        }
    }

    private func applyTopInset(previous: CGFloat) {
        guard let scrollView = contentView?.scrollView else { return }
        let wasAtTop = scrollView.contentOffset.y <= -previous
        scrollView.contentInset.top = topInset
        scrollView.verticalScrollIndicatorInsets.top = topInset
        if wasAtTop {
            scrollView.contentOffset.y = -topInset
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
