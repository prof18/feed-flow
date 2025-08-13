//
//  Ios26Checker.swift
//  Reader
//
//  Created by Marco Gomiero on 13/08/25.
//

func isiOS26OrLater() -> Bool {
    if #available(iOS 26.0, *) {
        return true
    } else {
        return false
    }
}
