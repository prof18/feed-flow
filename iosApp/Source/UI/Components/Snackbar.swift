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
    @Binding
    var messageQueue: Deque<SnackbarData>

    @State
    private var snackbarData: SnackbarData = .init()

    @State
    private var showBanner: Bool = false

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: Spacing.xsmall) {
                Text(snackbarData.title)
                    .font(.title3)
                    .foregroundColor(Color.popupText)

                if let subtitle = snackbarData.subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(Color.popupText)
                }
            }
            .padding(.vertical, Spacing.regular)
            .padding(.horizontal, Spacing.medium)

            Spacer()
        }
        .background(Color.popupBackground)
        .shadow(radius: 10)
        .cornerRadius(8)
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
