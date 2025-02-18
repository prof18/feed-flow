//
//  VisionOsUtils.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 02.04.24.
//  Copyright © 2024. All rights reserved.
//

import Foundation

func isOnVisionOSDevice() -> Bool {
    let isOnVisionOSDevice = NSClassFromString("UIWindowSceneGeometryPreferencesVision") != nil
    return isOnVisionOSDevice
}
