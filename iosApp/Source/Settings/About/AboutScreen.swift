//
//  About.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 22/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared

struct AboutScreen: View {

    @Environment(\.openURL)
    private var openURL

    @State
    private var showLicensesSheet = false

    @State
    private var licensesContent: String = ""

    var body: some View {

        VStack {
            Text(localizer.about_the_app.localized)
                .padding(Spacing.regular)
                .font(.system(size: 16))

            Button(
                localizer.open_website_button.localized,
                action: {
                    if let url = URL(string: Websites.shared.FEED_FLOW_WEBSITE) {
                        self.openURL(url)
                    }
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)

            Button(
                localizer.open_source_licenses.localized,
                action: {
                    let baseURL = Bundle.main.url(forResource: "licenses", withExtension: "html")!
                    let htmlString = try? String(contentsOf: baseURL, encoding: String.Encoding.utf8)

                    self.licensesContent = htmlString ?? ""
                    self.showLicensesSheet.toggle()
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)

            Spacer()

            let authorLink: LocalizedStringKey = """
                \(localizer.author_label.localized) [Marco Gomiero](https://www.marcogomiero.com)
            """
            Text(authorLink)
        }.sheet(isPresented: $showLicensesSheet) {
            LicensesScreen(htmlContent: licensesContent)
        }
        .navigationTitle(localizer.about_nav_bar.localized)
        .navigationBarTitleDisplayMode(.inline)

    }
}
