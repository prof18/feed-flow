//
//  FilePickerController.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import MobileCoreServices
import SwiftUI
import UniformTypeIdentifiers

struct FilePickerController: UIViewControllerRepresentable {
    var callback: (URL) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func updateUIViewController(
        _: UIDocumentPickerViewController,
        context _: UIViewControllerRepresentableContext<FilePickerController>
    ) {
        // Update the controller
    }

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let supportedTypes: [UTType] = [UTType.item]
        let controller = UIDocumentPickerViewController(forOpeningContentTypes: supportedTypes, asCopy: true)

        controller.delegate = context.coordinator
        return controller
    }

    class Coordinator: NSObject, UIDocumentPickerDelegate {
        var parent: FilePickerController

        init(_ pickerController: FilePickerController) {
            parent = pickerController
        }

        func documentPicker(_: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            parent.callback(urls[0])
        }

        func documentPickerWasCancelled() {}
    }
}
