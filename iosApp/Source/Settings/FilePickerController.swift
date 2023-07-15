//
//  FilePickerController.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import MobileCoreServices
import UniformTypeIdentifiers

struct FilePickerController: UIViewControllerRepresentable {
    var callback: (URL) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func updateUIViewController(
        _ uiViewController: UIDocumentPickerViewController,
        context: UIViewControllerRepresentableContext<FilePickerController>
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
            self.parent = pickerController
        }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            self.parent.callback(urls[0])
        }

        func documentPickerWasCancelled() {
        }
    }
}
