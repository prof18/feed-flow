//
//  ShareSheet.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 09/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI

struct ShareSheet: UIViewControllerRepresentable {
    typealias Callback = (
        _ activityType: UIActivity.ActivityType?,
        _ completed: Bool,
        _ returnedItems: [Any]?,
        _ error: Error?
    ) -> Void

    let activityItems: [Any]
    let applicationActivities: [UIActivity]?
    let onSelectedCallback: Callback?

    func makeUIViewController(
        context _: UIViewControllerRepresentableContext<ShareSheet>
    ) -> UIActivityViewController {
        let controller = UIActivityViewController(
            activityItems: activityItems,
            applicationActivities: applicationActivities
        )
        controller.completionWithItemsHandler = onSelectedCallback
        return controller
    }

    func updateUIViewController(
        _: UIActivityViewController,
        context _: UIViewControllerRepresentableContext<ShareSheet>
    ) {}
}
