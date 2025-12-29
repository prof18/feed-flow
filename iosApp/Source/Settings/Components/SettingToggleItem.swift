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
    let systemImage: String
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
            Label(title, systemImage: systemImage)
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
