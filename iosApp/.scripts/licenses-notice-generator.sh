#!/bin/sh

echo "Checking if license-plist is installed"
brew list licenseplist || brew install licenseplist

echo "Generating html licenses"
license-plist --html-path ./Assets/licenses.html --force

echo "Remove unused files"
rm -rf com.mono0926.LicensePlist.Output