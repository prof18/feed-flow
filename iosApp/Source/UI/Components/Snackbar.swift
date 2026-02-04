//
//  Snackbar.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Collections
import SwiftUI

struct Snackbar: View {
    @Binding var messageQueue: Deque<SnackbarData>

    @State private var snackbarData: SnackbarData = .init()

    @State private var showBanner = false
    @State private var dismissDirection: SnackbarDismissDirection = .vertical

    var body: some View {
        HStack(spacing: Spacing.medium) {
            VStack(alignment: .leading, spacing: Spacing.xsmall) {
                Text(snackbarData.title)
                    .font(.system(.body, design: .rounded, weight: .semibold))
                    .foregroundColor(.white)
                    .fixedSize(horizontal: false, vertical: true)

                if let subtitle = snackbarData.subtitle {
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

                    if horizontalSwipe {
                        dismissDirection = .horizontal(direction: value.translation.width >= 0 ? 1 : -1)
                        withAnimation {
                            showBanner = false
                        }
                    }
                }
        )
        .onChange(of: messageQueue) {
            if let data = self.messageQueue.first {
                dismissDirection = .vertical
                withAnimation {
                    self.snackbarData = data
                    self.showBanner = true
                }

                DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
                    withAnimation {
                        dismissDirection = .vertical
                        showBanner = false
                        if !self.messageQueue.isEmpty {
                            self.messageQueue.removeFirst()
                        }
                    }
                }
            }
        }
        .padding(.bottom, Spacing.regular)
        .zIndex(100)
        .offset(showBanner ? .zero : hiddenOffset)
        .padding(.horizontal, Spacing.medium)
    }

    private var hiddenOffset: CGSize {
        switch dismissDirection {
        case .horizontal(let direction):
            return CGSize(width: direction * UIScreen.main.bounds.width, height: 0)
        case .vertical:
            return CGSize(width: 0, height: UIScreen.main.bounds.height)
        }
    }
}

private enum SnackbarDismissDirection {
    case horizontal(direction: CGFloat)
    case vertical
}
