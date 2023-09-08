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
                localizer.refresh_feeds.localized,
                action: {
                    onReloadClick()
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
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
