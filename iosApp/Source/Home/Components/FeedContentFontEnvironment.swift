//
//  FeedContentFontEnvironment.swift
//  FeedFlow
//

import FeedFlowKit
import SwiftUI

private struct FeedBodyFontKey: EnvironmentKey {
    static let defaultValue: ReaderFontFamily = .system
}

private struct FeedHeadlineFontKey: EnvironmentKey {
    static let defaultValue: ReaderFontFamily = .system
}

extension EnvironmentValues {
    var feedBodyFont: ReaderFontFamily {
        get { self[FeedBodyFontKey.self] }
        set { self[FeedBodyFontKey.self] = newValue }
    }

    var feedHeadlineFont: ReaderFontFamily {
        get { self[FeedHeadlineFontKey.self] }
        set { self[FeedHeadlineFontKey.self] = newValue }
    }
}
