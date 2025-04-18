import Foundation
import SwiftUI
import Fuzi

extension HTMLDocument {
    public convenience init(stringSAFE: String) throws {
        try self.init(data: Data(stringSAFE.utf8))
    }
}

extension String {
    var asJSString: String {
        let data = try! JSONSerialization.data(withJSONObject: self, options: .fragmentsAllowed)
        return String(data: data, encoding: .utf8)!
    }

    var byStrippingSiteNameFromPageTitle: String {
        for separator in [" | ", " – ", " — ", " - "] {
            if self.contains(separator), let firstComponent = components(separatedBy: separator).first, firstComponent != "" {
                return firstComponent.byStrippingSiteNameFromPageTitle
            }
        }
        return self
    }

    var nilIfEmpty: String? {
        return isEmpty ? nil : self
    }
}

extension URL {
    var inferredFaviconURL: URL {
        return URL(string: "/favicon.ico", relativeTo: self)!
    }

    var hostWithoutWWW: String {
        var parts = (host ?? "").components(separatedBy: ".")
        if parts.first == "www" {
            parts.remove(at: 0)
        }
        return parts.joined(separator: ".")
    }
}

extension View {
    func onAppearOrChange<T: Equatable>(_ value: T, perform: @escaping (T) -> Void) -> some View {
        self.onAppear(perform: { perform(value) }).onChange(of: value, perform: perform)
    }
}

func assertNotOnMainThread() {
    #if DEBUG
    assert(!Thread.isMainThread)
    #endif
}
