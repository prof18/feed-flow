//
//  SettingToggleItem.swift
//  FeedFlow
//
//  Created by Marco Gomiero
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct SettingToggleItem: View {
    @Binding var isOn: Bool

    let title: String
    var systemImage: String?
    var confirmationDialog: ConfirmationDialogConfig?

    @State private var showConfirmation = false
    @State private var pendingValue = false

    var body: some View {
        Toggle(isOn: Binding(
            get: { isOn },
            set: { newValue in
                if newValue, let config = confirmationDialog {
                    pendingValue = newValue
                    showConfirmation = true
                } else {
                    isOn = newValue
                }
            }
        )) {
            if let systemImage {
                Label(title, systemImage: systemImage)
            } else {
                Text(title)
            }
        }
        .onTapGesture {
            let newValue = !isOn
            if newValue, let config = confirmationDialog {
                pendingValue = newValue
                showConfirmation = true
            } else {
                isOn = newValue
            }
        }
        .confirmationDialog(
            title: confirmationDialog?.title ?? "",
            message: confirmationDialog?.message ?? "",
            isPresented: $showConfirmation,
            onConfirm: {
                isOn = pendingValue
            }
        )
    }
}

struct ConfirmationDialogConfig {
    let title: String
    let message: String
}
