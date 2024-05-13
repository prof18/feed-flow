#!/bin/bash

# Check if the root folder name is provided as an argument
if [ $# -eq 0 ]; then
    echo "Please provide the root folder name as an argument."
    exit 1
fi

# Root folder name provided as input
root_folder="$1"

# Create the folder structure
mkdir -p "${root_folder}"
mkdir -p "${root_folder}/android/phone"
mkdir -p "${root_folder}/android/tablet"
mkdir -p "${root_folder}/ios/ipad"
mkdir -p "${root_folder}/ios/iphone"
mkdir -p "${root_folder}/ios/iphone-small"

# Output the folder structure
echo "Folder structure created:"
tree "${root_folder}"
