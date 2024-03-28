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

    private let authorLink: LocalizedStringKey = "\(feedFlowStrings.authorLabel) [Marco Gomiero](https://www.marcogomiero.com)"

    var body: some View {
        VStack {
            List {
                Section {
                    Text(feedFlowStrings.aboutTheApp)
                        .padding(.vertical, Spacing.small)
                        .font(.system(size: 16))
                        .accessibilityIdentifier(TestingTag.shared.ABOUT_TOOLBAR)

                    NavigationLink(destination: LicensesScreen()) {
                        Label(feedFlowStrings.openSourceLicenses, systemImage: "shield" )
                    }

                    Link(destination: URL(string: Websites.shared.FEED_FLOW_WEBSITE)!) {
                        Label(feedFlowStrings.openWebsiteButton, systemImage: "globe")
                    }

                    Link(destination: URL(string: Websites.shared.TRANSLATION_WEBSITE)!) {
                        Label(feedFlowStrings.aboutMenuContributeTranslations, systemImage: "flag")
                    }
                } footer: {
                    if let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
                        Text(feedFlowStrings.aboutAppVersion(appVersion))
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, Spacing.small)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Color.secondaryBackgroundColor)

            Spacer()

            Text(authorLink)
                .padding(.bottom, Spacing.small)
        }
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.aboutNavBar))
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    AboutScreen()
}
