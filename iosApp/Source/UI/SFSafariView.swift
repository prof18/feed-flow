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

    func makeUIViewController(context _: UIViewControllerRepresentableContext<Self>) -> SFSafariViewController {
        return SFSafariViewController(url: url)
    }

    func updateUIViewController(
        _: SFSafariViewController,
        context _: UIViewControllerRepresentableContext<SFSafariView>
    ) {
        // No need to do anything here
    }
}
