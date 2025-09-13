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
                .onChanged { _ in
                    withAnimation {
                        showBanner = false
                    }
                }
        )
        .onChange(of: messageQueue) {
            if let data = self.messageQueue.first {
                withAnimation {
                    self.snackbarData = data
                    self.showBanner = true
                }

                DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
                    withAnimation {
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
        .offset(y: showBanner ? 0 : UIScreen.main.bounds.height)
        .padding(.horizontal, Spacing.medium)
    }
}
