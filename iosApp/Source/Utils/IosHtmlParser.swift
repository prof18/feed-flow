//
//  IosHtmlParser.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import shared
import SwiftSoup

class IosHtmlParser: HtmlParser {
    
    func getTextFromHTML(html: String) -> String? {
        do {
            let doc: Document = try SwiftSoup.parse(html)
            return try doc.text()
        } catch {
            // TODO: replace with kermit?
            print("Error during html parsing: \(error)")
            return nil
        }
    }
}
