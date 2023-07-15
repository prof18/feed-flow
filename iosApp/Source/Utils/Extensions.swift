//
//  Extensions.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 15/07/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import shared

extension StringResource {
    var localized: String {
        return self.desc().localized()
   }
}
