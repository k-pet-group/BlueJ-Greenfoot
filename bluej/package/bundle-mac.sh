#!/bin/sh
#
# Args:
#   $1 - path to zip directory
#        (contains Greenfoot.app/BlueJ.app)
#   $2 - App bundle name ("Greenfoot.app" or "BlueJ.app")
#   $3 - path to JDK to bundle
#   $4 - destination zipfile name

# Copy JDK:

cp -r "$3"/* "$1/$2/Contents/JDK" || exit 1

# Sign the app:

codesign --verbose -s "Developer ID Application: Michael Kolling" "$1/$2/Contents/JDK/" &&
    codesign --verbose -s "Developer ID Application: Michael Kolling" "$1/$2" &&
    codesign --verify --deep --verbose=4 "$1/$2" &&
    spctl -a -t exec -vv "$1/$2" || echo " *** failed to sign or verify Mac bundle ***"

# Zip it:

cd "`dirname "$1"`" || exit 6
zip -q -y -r "$4" "`basename "$1"`" || exit 7
