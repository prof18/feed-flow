//
//  LicensesScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 22/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared

struct LicensesScreen: View {

    @Environment(\.presentationMode)
    private var presentationMode

    @State
    var htmlContent: String?

    var body: some View {
        licenseView
            .padding(Spacing.regular)
            .onAppear {
                let baseURL = Bundle.main.url(forResource: "licenses", withExtension: "html")!
                let htmlString = try? String(contentsOf: baseURL, encoding: String.Encoding.utf8)

                self.htmlContent = htmlString ?? ""
            }
    }

    @ViewBuilder
    private var licenseView: some View {
        if let content = htmlContent {
            HTMLStringView(
                htmlContent: content
            )
        } else {
            Spacer()

            ProgressView()
            Spacer()
        }
    }

}

struct LicensesScreen_Previews: PreviewProvider {
    static var previews: some View {
        LicensesScreen(
            htmlContent: """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt \
                ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco \
                laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate \
                velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, \
                sunt in culpa qui officia deserunt mollit anim id est laborum.

                """
        )
    }
}
