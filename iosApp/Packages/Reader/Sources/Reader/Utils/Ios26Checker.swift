//
//  Ios26Checker.swift
//  Reader
//
//  Created by Marco Gomiero on 22/01/25.
//

func isiOS26OrLater() -> Bool {
    #if os(iOS)
    if #available(iOS 26.0, *) {
        return true
    }
    return false
    #else
    return false
    #endif
}
