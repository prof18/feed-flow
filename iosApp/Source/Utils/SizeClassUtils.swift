//
//  SizeClassUtils.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 23/10/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//
import SwiftUI

func compactToolbarHeaderWidth(verticalSizeClass: UserInterfaceSizeClass?) -> CGFloat {
    verticalSizeClass == .compact ? 320 : 148
}
