import SwiftUI
import UIKit

extension String {
    func loadImageFromCache() -> UIImage? {
        // Try to get from cache first
        if let cachedImage = ImageCache.shared.get(forKey: self) {
            return cachedImage
        }

        // If not in cache, try to load synchronously
        guard let url = URL(string: self) else { return nil }
        guard let data = try? Data(contentsOf: url) else { return nil }
        guard let image = UIImage(data: data) else { return nil }

        // Resize image before caching to ensure it's not too large for widgets
        let resizedImage = image.resized(toWidth: 500)

        // Store in cache for future use
        if let image = resizedImage {
            ImageCache.shared.set(image, forKey: self)
        }

        return resizedImage
    }
}

extension UIImage {
    func resized(toWidth width: CGFloat, isOpaque: Bool = true) -> UIImage? {
        let canvas = CGSize(width: width, height: CGFloat(ceil(width / size.width * size.height)))
        let format = imageRendererFormat
        format.opaque = isOpaque
        return UIGraphicsImageRenderer(size: canvas, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: canvas))
        }
    }
}

class ImageCache {
    static let shared = ImageCache()
    private var cache = NSCache<NSString, UIImage>()

    private init() {
        // Configure cache limits
        cache.countLimit = 100
    }

    func set(_ image: UIImage, forKey key: String) {
        cache.setObject(image, forKey: key as NSString)
    }

    func get(forKey key: String) -> UIImage? {
        return cache.object(forKey: key as NSString)
    }

    func remove(forKey key: String) {
        cache.removeObject(forKey: key as NSString)
    }

    func clear() {
        cache.removeAllObjects()
    }
}
