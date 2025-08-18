//
//  SnackbarModifier.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 18/08/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import Collections
import SwiftUI

struct SnackbarModifier: ViewModifier {
    @Binding var messageQueue: Deque<SnackbarData>

    func body(content: Content) -> some View {
        ZStack {
            content

            VStack(spacing: 0) {
                Spacer()

                Snackbar(messageQueue: $messageQueue)
            }
        }
    }
}

extension View {
    func snackbar(messageQueue: Binding<Deque<SnackbarData>>) -> some View {
        modifier(SnackbarModifier(messageQueue: messageQueue))
    }
}
