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
        context.coordinator.loadImage(from: url)
        context.coordinator.onImageLoaded = onImageLoaded
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

            // Check cache first
            let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
            if let cachedResponse = urlSession.configuration.urlCache?.cachedResponse(for: request),
               let image = UIImage(data: cachedResponse.data) {
                DispatchQueue.main.async { [weak self] in
                    self?.imageView?.image = image
                    self?.onImageLoaded?(image)
                }
                return
            }

            // Download if not cached
            currentTask = urlSession.dataTask(with: request) { [weak self] data, _, _ in
                guard let data, let image = UIImage(data: data) else { return }
                DispatchQueue.main.async {
                    self?.imageView?.image = image
                    self?.onImageLoaded?(image)
                }
            }
            currentTask?.resume()
        }
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
