//
//  ImageLoadingView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 orgName. All rights reserved.
//


import SwiftUI

struct ImageLoadingView: View {
    
    let urlString: String
    let size: CGFloat
    
    var body: some View {
        
        AsyncImage(url: URL(string: urlString)) { phase in
            switch phase {
            case .empty:
                ProgressView()
                    .frame(width: size)
            case .failure(_):
                EmptyView()
//                    .frame(width: 1, height: 1, alignment: .center)
            case .success(let image):
                image
                    .resizable()
                    .scaledToFill()
                    .frame(width: size)
//                    .frame(width: size, height: size, alignment: .center)
                    .cornerRadius(16)
                    .clipped()
//                    .border(Color(white: 0.8))
            @unknown default:
                EmptyView()
            }
        }
    }
}

struct ImageLoadingView_Previews: PreviewProvider {
    static var previews: some View {
        ImageLoadingView(urlString: "", size: 100)
    }
}
