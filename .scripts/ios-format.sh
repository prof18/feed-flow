#!/bin/sh

swiftformat iosApp  --swiftversion 5
swiftlint --fix iosApp
swiftlint iosApp
