//
//  Snackbar.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI

struct Snackbar: View {
    let message: SnackbarData
    let isExiting: Bool
    let onDismiss: () -> Void

    @State private var dismissDirection: SnackbarDismissDirection = .vertical
    @State private var snackbarSize = CGSize.zero

    var body: some View {
        HStack(spacing: Spacing.medium) {
            VStack(alignment: .leading, spacing: Spacing.xsmall) {
                Text(message.title)
                    .font(.system(.body, design: .rounded, weight: .semibold))
                    .foregroundColor(.white)
                    .fixedSize(horizontal: false, vertical: true)

                if let subtitle = message.subtitle {
                    Text(subtitle)
                        .font(.system(.footnote, design: .rounded))
                        .foregroundColor(.white.opacity(0.8))
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            Spacer()
        }
        .padding(.vertical, Spacing.medium)
        .padding(.horizontal, Spacing.medium)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(
                    LinearGradient(
                        colors: [
                            Color(red: 0.15, green: 0.15, blue: 0.2).opacity(0.92),
                            Color(red: 0.1, green: 0.1, blue: 0.15).opacity(0.88)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.3), radius: 20, x: 0, y: 10)
        .shadow(color: .black.opacity(0.1), radius: 5, x: 0, y: 2)
        .transition(AnyTransition.move(edge: .bottom).combined(with: .opacity))
        .gesture(
            DragGesture()
                .onEnded { value in
                    let horizontalDismissThreshold: CGFloat = 40
                    let horizontalSwipe = abs(value.translation.width) > horizontalDismissThreshold &&
                        abs(value.translation.width) > abs(value.translation.height)

                    if horizontalSwipe, !isExiting {
                        let direction: CGFloat = value.translation.width >= 0 ? 1 : -1
                        withAnimation {
                            dismissDirection = .horizontal(direction: direction)
                            onDismiss()
                        }
                    }
                }
        )
        .onGeometryChange(for: CGSize.self) { proxy in
            proxy.size
        } action: { newSize in
            if snackbarSize != newSize {
                snackbarSize = newSize
            }
        }
        .padding(.bottom, Spacing.regular)
        .zIndex(100)
        .opacity(isExiting ? 0 : 1)
        .allowsHitTesting(!isExiting)
        .accessibilityElement(children: .combine)
        .accessibilityHidden(isExiting)
        .accessibilityIdentifier(SnackbarAccessibilityIdentifiers.banner)
        .offset(isExiting ? hiddenOffset : .zero)
        .padding(.horizontal, Spacing.medium)
    }

    private var hiddenOffset: CGSize {
        switch dismissDirection {
        case .horizontal(let direction):
            return CGSize(width: direction * (snackbarSize.width + Spacing.medium), height: 0)
        case .vertical:
            return CGSize(width: 0, height: snackbarSize.height + Spacing.regular)
        }
    }
}

private enum SnackbarDismissDirection {
    case horizontal(direction: CGFloat)
    case vertical
}
