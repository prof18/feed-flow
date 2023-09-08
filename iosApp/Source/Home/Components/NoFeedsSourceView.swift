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
                localizer.add_feed.localized,
                action: {
                    onAddFeedClick()
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
        }
    }
}

struct NoFeedsSourceView_Previews: PreviewProvider {
    static var previews: some View {
        NoFeedsSourceView(
            onAddFeedClick: {}
        )
    }
}
