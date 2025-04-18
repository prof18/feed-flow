//
//  WebViewEvent.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//
import Foundation

enum WebViewEvent: Equatable {
    struct ScrollInfo: Equatable {
        var contentOffset: CGPoint
        var contentSize: CGSize
    }

    case scrolledDown
    case scrolledUp
    case scrollPositionChanged(ScrollInfo)
}
