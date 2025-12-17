import Foundation
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
    ) {}

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let supportedTypes: [UTType] = [.item]
        let controller = UIDocumentPickerViewController(forOpeningContentTypes: supportedTypes, asCopy: true)
        controller.delegate = context.coordinator
        return controller
    }

    final class Coordinator: NSObject, UIDocumentPickerDelegate {
        private let parent: FilePickerController

        init(_ pickerController: FilePickerController) {
            parent = pickerController
        }

        func documentPicker(_: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else { return }
            parent.callback(url)
        }

        func documentPickerWasCancelled(_: UIDocumentPickerViewController) {}
    }
}

