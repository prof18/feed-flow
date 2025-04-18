//
//  FetchAndExtractionResult.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//
import SwiftUI

struct FetchAndExtractionResult {
    public var metadata: SiteMetadata?
    public var extracted: ExtractedContent
    public var styledHTML: String
    public var baseURL: URL

    public var title: String? {
        extracted.title?.nilIfEmpty ?? metadata?.title?.nilIfEmpty
    }
}
