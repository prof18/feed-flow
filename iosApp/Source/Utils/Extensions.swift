//
//  Extensions.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 15/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Nuke
import SwiftUI
import UIKit

extension View {
    func modify<Content>(@ViewBuilder _ transform: (Self) -> Content) -> Content {
        transform(self)
    }
}

extension String {
    func truncate(maxChar: Int) -> String {
        if count <= maxChar {
            return self
        }
        return prefix(maxChar) + "..."
    }
}

extension UIViewController {
    var topmostPresented: UIViewController {
        presentedViewController?.topmostPresented ?? self
    }
}

extension ImageRequest {
    static func resized(url: URL?, size: CGSize) -> ImageRequest? {
        guard let url else { return nil }
        return ImageRequest(
            url: url,
            processors: [.resize(size: size, contentMode: .aspectFill)]
        )
    }
}
