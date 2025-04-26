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
    public var readerExtractor: ReaderExtractorType

    public init(
        additionalCSS: String,
        onLinkClicked: ((URL) -> Void)? = nil,
        readerExtractor: ReaderExtractorType
    ) {
        self.additionalCSS = additionalCSS
        self.onLinkClicked = onLinkClicked
        self.readerExtractor = readerExtractor
    }
}
