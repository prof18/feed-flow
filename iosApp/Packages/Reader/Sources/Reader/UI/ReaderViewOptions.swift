//
//  ReaderViewOptions.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//
import SwiftUI

public struct ReaderViewOptions {
    public var additionalCSS: String?
    public var onLinkClicked: ((URL) -> Void)?
    public init(additionalCSS _: String? = nil, onLinkClicked: ((URL) -> Void)? = nil) {
        self.onLinkClicked = onLinkClicked
    }
}
