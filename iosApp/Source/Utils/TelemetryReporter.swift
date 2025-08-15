//
//  TelemetryReporter.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 15/08/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import TelemetryDeck

class TelemetryReporter: Telemetry {
    func signal(id: String) {
        #if !DEBUG
            TelemetryDeck.signal(id)
        #endif
    }

    func trackError(id: String, message: String) {
        #if !DEBUG
            TelemetryDeck.errorOccurred(id: id, message: message)
        #endif
    }
}
