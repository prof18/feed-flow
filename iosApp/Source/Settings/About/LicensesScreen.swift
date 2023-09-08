//
//  LicensesScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 22/07/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI
import shared

struct LicensesScreen: View {

    @Environment(\.presentationMode) var presentationMode
    let htmlContent: String

    var body: some View {
        NavigationStack {
            HTMLStringView(
                htmlContent: htmlContent
            )
            .padding(Spacing.regular)
            .toolbar {

                ToolbarItem(placement: .navigationBarLeading) {
                    Button(
                        action: {
                            self.presentationMode.wrappedValue.dismiss()
                        },
                        label: {
                            Image(systemName: "xmark")
                        }
                    )
                }

                ToolbarItem(placement: .navigationBarLeading) {
                    Text(localizer.open_source_nav_bar.localized)
                        .font(.title2)
                        .padding(.vertical, Spacing.medium)
                }
            }
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
