#!/bin/sh

cd iosApp
swiftformat --swiftversion 5
swiftlint --fix
swiftlint
cd ..
