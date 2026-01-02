//
//  LicensesScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 22/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct LicensesScreen: View {
    @Environment(\.presentationMode)
    private var presentationMode

    @State var htmlContent: String?

    var body: some View {
        licenseView
            .padding(Spacing.regular)
            .onAppear {
                guard let baseURL = Bundle.main.url(forResource: "licenses", withExtension: "html") else { return }
                let htmlString = try? String(contentsOf: baseURL, encoding: String.Encoding.utf8)

                self.htmlContent = htmlString ?? ""
            }
    }

    @ViewBuilder private var licenseView: some View {
        if let content = htmlContent {
            HTMLStringView(htmlContent: content)
        } else {
            Spacer()
            ProgressView()
            Spacer()
        }
    }
}
