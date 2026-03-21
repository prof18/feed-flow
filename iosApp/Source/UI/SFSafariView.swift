//
//  SFSafariView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 17/04/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import SafariServices
import SwiftUI

struct SFSafariView: UIViewControllerRepresentable {
    let url: URL
    var onDismiss: (() -> Void)?
    @Environment(\.dismiss)
    private var dismiss

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let safari = SFSafariViewController(url: url)
        safari.delegate = context.coordinator
        return safari
    }

    func updateUIViewController(
        _: SFSafariViewController,
        context _: Context
    ) {
        // No need to do anything here
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, SFSafariViewControllerDelegate {
        var parent: SFSafariView

        init(_ parent: SFSafariView) {
            self.parent = parent
        }

        func safariViewControllerDidFinish(_: SFSafariViewController) {
            if let onDismiss = parent.onDismiss {
                onDismiss()
            } else {
                parent.dismiss()
            }
        }
    }
}
