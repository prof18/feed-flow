//
//  EmptyFeedView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI

struct EmptyFeedView: View {
    
    let onReloadClick: () -> Void
    
    var body: some View {
        VStack {
            Text("Nothing else to read here!")
                .font(.body)
            
            Button(
                "Refresh Feeds",
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
