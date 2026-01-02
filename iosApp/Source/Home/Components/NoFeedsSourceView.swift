//
//  NoFeedsSourceView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct NoFeedsSourceView: View {
    let onAddFeedClick: () -> Void

    var body: some View {
        VStack {
            Text(feedFlowStrings.noFeedsFoundMessage)
                .font(.body)

            Button(
                action: {
                    onAddFeedClick()
                },
                label: {
                    Text(feedFlowStrings.addFeed)
                        .frame(maxWidth: .infinity)
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.medium)
        }
    }
}
