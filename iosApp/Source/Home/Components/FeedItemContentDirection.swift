//
//  FeedItemContentDirection.swift
//  FeedFlow
//
//  Copyright © 2026 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

extension FeedItem {
    /// The direction the article text itself reads in, or `nil` when its text has no strongly
    /// directional character and the app locale should keep deciding.
    ///
    /// Lets a single item be laid out right-to-left even while the UI is in a left-to-right
    /// locale, which is what a Persian or Hebrew feed needs in an otherwise English app.
    /// Resolved while mapping, so this only reads the stored value: `body` is evaluated on every
    /// render pass and must not cross into Kotlin to compute it.
    var contentLayoutDirection: LayoutDirection? {
        switch contentDirection {
        case .leftToRight:
            return .leftToRight
        case .rightToLeft:
            return .rightToLeft
        case nil:
            return nil
        }
    }
}
