//
//  NoFeedsSourceView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI

struct NoFeedsSourceView: View {
    let onAddFeedClick: () -> Void
    
    var body: some View {
        VStack {
            Text("No feeds found. Please add a new feed!")
                .font(.body)
            
            Button(
                "Add feed",
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
