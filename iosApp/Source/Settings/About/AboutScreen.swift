//
//  AboutScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 22/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import SwiftUI

struct AboutScreen: View {
    @Environment(\.openURL) private var openURL

    // swiftlint:disable line_length
    private let authorLink: LocalizedStringKey = "\(feedFlowStrings.authorLabel) [Marco Gomiero](https://www.marcogomiero.com)"
    // swiftlint:enable line_length

    var body: some View {
        VStack {
            List {
                Section {
                    Text(feedFlowStrings.aboutTheApp)
                        .padding(.vertical, Spacing.small)
                        .font(.system(size: 16))
                        .accessibilityIdentifier(TestingTag.shared.ABOUT_TOOLBAR)

                    NavigationLink(destination: LicensesScreen()) {
                        Label(feedFlowStrings.openSourceLicenses, systemImage: "shield")
                    }

                    Button(
                        action: {
                            if let url = URL(string: Websites.shared.FEED_FLOW_WEBSITE) {
                                openURL(url)
                            }
                        },
                        label: {
                            Label(feedFlowStrings.openWebsiteButton, systemImage: "globe")
                        }
                    )

                    Button(
                        action: {
                            if let url = URL(string: Websites.shared.TRANSLATION_WEBSITE) {
                                openURL(url)
                            }
                        },
                        label: {
                            Label(feedFlowStrings.aboutMenuContributeTranslations, systemImage: "flag")
                        }
                    )

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
