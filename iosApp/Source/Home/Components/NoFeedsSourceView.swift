//
//  NoFeedsSourceView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared

struct NoFeedsSourceView: View {
    let onAddFeedClick: () -> Void

    var body: some View {
        VStack {
            Text(localizer.no_feeds_found_message.localized)
                .font(.body)

            Button(
                action: {
                    onAddFeedClick()
                },
                label: {
                    Text(localizer.add_feed.localized)
                        .frame(maxWidth: .infinity)
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.medium)
            .accessibilityIdentifier(TestingTag.shared.HOME_SCREEN_ADD_FEED_BUTTON)
        }
    }
}

#Preview {
    NoFeedsSourceView(
        onAddFeedClick: {}
    )
}
