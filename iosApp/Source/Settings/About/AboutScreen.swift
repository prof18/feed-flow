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

    @Environment(\.openURL) private var openURL

    private let authorLink: LocalizedStringKey = "\(localizer.author_label.localized) [Marco Gomiero](https://www.marcogomiero.com)"

    var body: some View {
        VStack {
            List {
                Section {
                    Text(localizer.about_the_app.localized)
                        .padding(.vertical, Spacing.small)
                        .font(.system(size: 16))

                    NavigationLink(destination: LicensesScreen()) {
                        Label(localizer.open_source_licenses.localized, systemImage: "shield" )
                    }

                    Link(destination: URL(string: Websites.shared.FEED_FLOW_WEBSITE)!) {
                        Label(localizer.open_website_button.localized, systemImage: "globe")
                    }
                } footer: {
                    if let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
                        let appVersionString = LocalizationUtils.shared.formatString(
                            resource: MR.strings().about_app_version,
                            args: [appVersion]
                        )
                        Text(appVersionString)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, Spacing.small)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .padding(.top, -Spacing.medium)
            .background(Color.secondaryBackgroundColor)

            Spacer()

            Text(authorLink)
                .padding(.bottom, Spacing.small)
        }
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(localizer.about_nav_bar.localized))
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    AboutScreen()
}
