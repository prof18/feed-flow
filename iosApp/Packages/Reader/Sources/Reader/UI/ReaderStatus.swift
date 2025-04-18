//
//  ReaderStatus.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//

import Foundation

public enum ReaderStatus: Equatable {
    case fetching
    case failedToExtractContent
    case extractedContent(html: String, baseURL: URL, title: String?)
}
