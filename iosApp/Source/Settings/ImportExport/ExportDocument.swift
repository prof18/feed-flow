//
//  ExportDocument.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/09/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//
import Foundation
import SwiftUI
import UniformTypeIdentifiers

struct ExportDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.xml] }

    var data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}
