import SwiftUI
import UIKit

struct ReaderImageViewer: View {
    let imageUrl: URL
    let onClose: () -> Void
    @State private var showShareSheet = false
    @State private var imageToShare: UIImage?

    var body: some View {
        ZStack {
            Color.black
                .ignoresSafeArea()

            ZoomableImageView(url: imageUrl, onImageLoaded: { image in
                imageToShare = image
            })
            .ignoresSafeArea()
            .accessibilityIdentifier(ReaderImageViewerIds.viewer)

            VStack {
                HStack {
                    Button(
                        action: onClose,
                        label: {
                            Image(systemName: "xmark.circle.fill")
                                .font(.title)
                                .foregroundStyle(.white)
                        }
                    )
                    .accessibilityIdentifier(ReaderImageViewerIds.closeButton)
                    .padding()

                    Spacer()

                    Button(
                        action: {
                            showShareSheet = true
                        },
                        label: {
                            Image(systemName: "square.and.arrow.up.circle.fill")
                                .font(.title)
                                .foregroundStyle(.white)
                        }
                    )
                    .accessibilityIdentifier(ReaderImageViewerIds.shareButton)
                    .disabled(imageToShare == nil)
                    .padding()
                }
                Spacer()
            }
        }
        .sheet(isPresented: $showShareSheet) {
            if let image = imageToShare {
                ShareSheet(items: [image])
            }
        }
    }
}

private struct ZoomableImageView: UIViewRepresentable {
    let url: URL
    let onImageLoaded: (UIImage) -> Void

    func makeUIView(context: Context) -> UIScrollView {
        let scrollView = UIScrollView()
        scrollView.minimumZoomScale = 1.0
        scrollView.maximumZoomScale = 5.0
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.showsVerticalScrollIndicator = false
        scrollView.backgroundColor = UIColor.black
        scrollView.delegate = context.coordinator

        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.isUserInteractionEnabled = true
        scrollView.addSubview(imageView)

        NSLayoutConstraint.activate([
            imageView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            imageView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            imageView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            imageView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
            imageView.heightAnchor.constraint(equalTo: scrollView.heightAnchor)
        ])

        let doubleTapGesture = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleDoubleTap(_:))
        )
        doubleTapGesture.numberOfTapsRequired = 2
        imageView.addGestureRecognizer(doubleTapGesture)

        context.coordinator.imageView = imageView
        context.coordinator.scrollView = scrollView
        context.coordinator.onImageLoaded = onImageLoaded
        context.coordinator.loadImage(from: url)
        return scrollView
    }

    func updateUIView(_ uiView: UIScrollView, context: Context) {
        context.coordinator.onImageLoaded = onImageLoaded
        context.coordinator.loadImage(from: url)
    }

    static func dismantleUIView(_: UIScrollView, coordinator: Coordinator) {
        coordinator.cancelLoad()
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    final class Coordinator: NSObject, UIScrollViewDelegate {
        var imageView: UIImageView?
        var scrollView: UIScrollView?
        var onImageLoaded: ((UIImage) -> Void)?
        private var currentUrl: URL?
        private var currentTask: URLSessionDataTask?
        private lazy var urlSession: URLSession = {
            let config = URLSessionConfiguration.default
            config.urlCache = URLCache.shared
            config.requestCachePolicy = .returnCacheDataElseLoad
            return URLSession(configuration: config)
        }()

        func viewForZooming(in scrollView: UIScrollView) -> UIView? {
            imageView
        }

        @objc
        func handleDoubleTap(_ gesture: UITapGestureRecognizer) {
            guard let scrollView = scrollView else { return }

            if scrollView.zoomScale > scrollView.minimumZoomScale {
                scrollView.setZoomScale(scrollView.minimumZoomScale, animated: true)
            } else {
                let location = gesture.location(in: imageView)
                let zoomRect = zoomRectForScale(scale: scrollView.maximumZoomScale, center: location)
                scrollView.zoom(to: zoomRect, animated: true)
            }
        }

        private func zoomRectForScale(scale: CGFloat, center: CGPoint) -> CGRect {
            guard let scrollView = scrollView else { return .zero }

            var zoomRect = CGRect.zero
            zoomRect.size.height = scrollView.frame.size.height / scale
            zoomRect.size.width = scrollView.frame.size.width / scale
            zoomRect.origin.x = center.x - (zoomRect.size.width / 2.0)
            zoomRect.origin.y = center.y - (zoomRect.size.height / 2.0)
            return zoomRect
        }

        func cancelLoad() {
            currentTask?.cancel()
            currentTask = nil
            currentUrl = nil
        }

        func loadImage(from url: URL) {
            guard currentUrl != url else {
                // Image already loaded for this URL
                if let currentImage = imageView?.image {
                    onImageLoaded?(currentImage)
                }
                return
            }
            currentUrl = url
            currentTask?.cancel()

            let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
            currentTask = urlSession.dataTask(with: request) { [weak self] data, response, _ in
                guard let data,
                      let image = ReaderImageDecoder.decode(data: data, response: response) else { return }
                DispatchQueue.main.async { [weak self] in
                    guard let self, self.currentUrl == url else { return }
                    self.imageView?.image = image
                    self.onImageLoaded?(image)
                }
            }
            currentTask?.resume()
        }
    }
}

private enum ReaderImageDecoder {
    private static let maxEncodedBytes = 20 * 1_024 * 1_024
    private static let blockedMimeTypes: Set<String> = [
        "application/pdf",
        "image/exr",
        "image/openexr",
        "image/svg+xml",
        "image/x-exr",
        "application/xml",
        "text/xml"
    ]
    private static let blockedPathExtensions: Set<String> = [
        "exr",
        "pdf",
        "svg",
        "svgz"
    ]

    static func decode(data: Data, response: URLResponse?) -> UIImage? {
        guard data.count <= maxEncodedBytes,
              canAttemptDecode(data: data, response: response) else { return nil }

        return UIImage(data: data)
    }

    private static func canAttemptDecode(data: Data, response: URLResponse?) -> Bool {
        let mimeType = response?.mimeType?.lowercased()
        if let mimeType, blockedMimeTypes.contains(mimeType) {
            return false
        }

        if let pathExtension = response?.url?.pathExtension.lowercased(),
           blockedPathExtensions.contains(pathExtension) {
            return false
        }

        return !hasBlockedSignature(data)
    }

    private static func hasBlockedSignature(_ data: Data) -> Bool {
        hasBytes(data, at: 0, matching: [0x76, 0x2F, 0x31, 0x01]) ||
            hasBytes(data, at: 0, matching: Array("%PDF-".utf8)) ||
            hasSvgSignature(data)
    }

    private static func hasSvgSignature(_ data: Data) -> Bool {
        let prefix = data.prefix(512)
        guard let text = String(data: prefix, encoding: .utf8)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() else { return false }

        return text.hasPrefix("<svg") ||
            text.hasPrefix("<?xml") && text.contains("<svg")
    }

    private static func hasBytes(_ data: Data, at offset: Int, matching signature: [UInt8]) -> Bool {
        guard data.count >= offset + signature.count else { return false }
        for (index, byte) in signature.enumerated() where data[data.index(data.startIndex, offsetBy: offset + index)] != byte {
            return false
        }
        return true
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
