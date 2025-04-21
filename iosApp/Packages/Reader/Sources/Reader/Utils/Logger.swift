//
//  Logger.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//

protocol Logger {
    func info(_ string: String)
    func error(_ string: String)
}

struct PrintLogger: Logger {
    func info(_ string: String) {
        print("[Reader] ℹ️ \(string)")
    }

    func error(_ string: String) {
        print("[Reader] 🚨 \(string)")
    }
}
