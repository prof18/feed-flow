//
//  ConfirmationButton.swift
//  FeedFlow
//
//  Created by Marco Gomiero
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct ConfirmationButton: View {
    let title: String
    var systemImage: String?
    let dialogTitle: String
    let dialogMessage: String
    let onConfirm: () -> Void
    var isDestructive: Bool = true

    @State private var showConfirmation = false

    var body: some View {
        Button(role: isDestructive ? .destructive : nil) {
            showConfirmation = true
        } label: {
            if let systemImage {
                Label(title, systemImage: systemImage)
            } else {
                Text(title)
            }
        }
        .confirmationDialog(
            title: dialogTitle,
            message: dialogMessage,
            isPresented: $showConfirmation,
            isDestructive: isDestructive,
            onConfirm: onConfirm
        )
    }
}
