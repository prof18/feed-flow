import Foundation
import RevenueCat

struct RevenueCatSupport {
    private(set) static var isAvailable = false

    private init() {}

    static func configure() {
        guard
            let apiKey = Bundle.main.object(forInfoDictionaryKey: "RevenueCatApiKey") as? String,
            !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            !apiKey.contains("$(")
        else {
            return
        }

        #if DEBUG
            Purchases.logLevel = .debug
        #endif

        Purchases.configure(withAPIKey: apiKey)
        isAvailable = true
    }
}
