//
//  SizeClassUtils.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 23/10/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//
import UIKit

enum DeviceType {
    case ipad
    case iphoneLandscape
    case iphonePortrait
}

func getDeviceType() -> DeviceType {
    if UIDevice.current.userInterfaceIdiom == .pad {
        return .ipad
    } else {
        if UIDevice.current.orientation.isLandscape {
            return .iphoneLandscape
        } else {
            return .iphonePortrait
        }
    }
}
