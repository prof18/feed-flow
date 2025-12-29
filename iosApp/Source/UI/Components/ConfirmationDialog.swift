//
//  ConfirmationDialog.swift
//  FeedFlow
//
//  Created by Marco Gomiero
//  Copyright © 2024. All rights reserved.
//

import SwiftUI

struct ConfirmationDialogModifier: ViewModifier {
    @Binding var isPresented: Bool

    let title: String
    let message: String
    let onConfirm: () -> Void
    var isDestructive: Bool = false

    func body(content: Content) -> some View {
        content
            .alert(title, isPresented: $isPresented) {
                Button(feedFlowStrings.cancelButton, role: .cancel) { }
                Button(
                    feedFlowStrings.confirmButton,
                    role: isDestructive ? .destructive : nil
                ) {
                    onConfirm()
                }
            } message: {
                Text(message)
            }
    }
}

extension View {
    func confirmationDialog(
        title: String,
        message: String,
        isPresented: Binding<Bool>,
        isDestructive: Bool = false,
        onConfirm: @escaping () -> Void
    ) -> some View {
        modifier(ConfirmationDialogModifier(
            isPresented: isPresented,
            title: title,
            message: message,
            onConfirm: onConfirm,
            isDestructive: isDestructive
        ))
    }
}
