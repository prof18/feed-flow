//
//  EmptyFeedView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared

struct EmptyFeedView: View {

    let onReloadClick: () -> Void

    var body: some View {
        VStack {
            Text(localizer.empty_feed_message.localized)
                .font(.body)

            Button(
                action: {
                    onReloadClick()
                },
                label: {
                    Text(localizer.refresh_feeds.localized)
                        .frame(maxWidth: .infinity)
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.medium)
        }
    }
}

struct EmptyFeedView_Previews: PreviewProvider {
    static var previews: some View {
        EmptyFeedView(
            onReloadClick: {}
        )
    }
}
